# Delivery Zone Consolidation Service

A REST API that groups restaurant partners by overlapping delivery zones, reducing the number of distinct geographic targets needed for marketing campaigns and ad-platform targeting. Restaurants whose delivery circles intersect (including transitively) are consolidated into a single group with a recommended covering target location and radius.

---

## Quick Start

**Prerequisites:** Docker, Java 21, Maven (or use the included wrapper)

```bash
# 1. Start PostgreSQL + PostGIS
docker compose up -d

# 2. Run the service (Flyway migrations run automatically on startup)
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`. Health check: `GET /actuator/health`.

---

## API Reference

### POST /restaurants

Upload (and fully replace) the restaurant dataset. Triggers group recomputation synchronously inside the same transaction.

**Request body** — JSON array:

```json
[
  {
    "id": "r1",
    "name": "Restaurant A",
    "latitude": 51.5074,
    "longitude": -0.1278,
    "deliveryRadiusMeters": 3000
  }
]
```

Field constraints:
- `id` — non-empty string, unique within the payload
- `name` — non-empty string
- `latitude` — `-90` to `90`
- `longitude` — `-180` to `180`
- `deliveryRadiusMeters` — non-negative integer

**Response `200 OK`:**

```json
{
  "status": "success",
  "restaurantsLoaded": 4,
  "groupCount": 2,
  "message": "Restaurants successfully stored"
}
```

**Error responses:**
- `400` — validation failure (field errors returned), duplicate IDs, or malformed JSON
- `500` — unexpected server error (no stack trace exposed)

> **Warning:** sending an empty array `[]` is accepted and will delete all existing restaurant and group data, returning `200` with `restaurantsLoaded: 0`. This is intentional — the endpoint fully replaces the dataset — but callers should guard against accidentally posting an empty payload.

```bash
curl -X POST http://localhost:8080/restaurants \
  -H "Content-Type: application/json" \
  -d '[
    {"id":"r1","name":"Restaurant A","latitude":51.5074,"longitude":-0.1278,"deliveryRadiusMeters":3000},
    {"id":"r2","name":"Restaurant B","latitude":51.5100,"longitude":-0.1300,"deliveryRadiusMeters":2500},
    {"id":"r3","name":"Restaurant C","latitude":51.5090,"longitude":-0.1250,"deliveryRadiusMeters":2000},
    {"id":"r4","name":"Restaurant D","latitude":51.6000,"longitude":-0.3000,"deliveryRadiusMeters":1500}
  ]'
```

---

### GET /groups

Retrieve all precomputed delivery zone groups, ordered by `groupId`.

**Response `200 OK`:**

```json
{
  "groupCount": 2,
  "groups": [
    {
      "groupId": "group_r1",
      "restaurantCount": 3,
      "restaurantIds": ["r1", "r2", "r3"],
      "recommendedTargetLatitude": 51.5088,
      "recommendedTargetLongitude": -0.1276,
      "recommendedTargetRadiusMeters": 3157
    },
    {
      "groupId": "group_r4",
      "restaurantCount": 1,
      "restaurantIds": ["r4"],
      "recommendedTargetLatitude": 51.6000,
      "recommendedTargetLongitude": -0.3000,
      "recommendedTargetRadiusMeters": 1500
    }
  ]
}
```

**Error responses:**
- `404` — no restaurant data has been loaded yet

```bash
curl http://localhost:8080/groups
```

---

### GET /groups/{groupId}

Retrieve a single group with full restaurant details.

**Response `200 OK`:**

```json
{
  "groupId": "group_r1",
  "restaurantCount": 3,
  "restaurantIds": ["r1", "r2", "r3"],
  "recommendedTargetLatitude": 51.5088,
  "recommendedTargetLongitude": -0.1276,
  "recommendedTargetRadiusMeters": 3157,
  "restaurants": [
    {"id": "r1", "name": "Restaurant A", "latitude": 51.5074, "longitude": -0.1278, "deliveryRadiusMeters": 3000},
    {"id": "r2", "name": "Restaurant B", "latitude": 51.5100, "longitude": -0.1300, "deliveryRadiusMeters": 2500},
    {"id": "r3", "name": "Restaurant C", "latitude": 51.5090, "longitude": -0.1250, "deliveryRadiusMeters": 2000}
  ]
}
```

**Error responses:**
- `404` — group ID does not exist

```bash
curl http://localhost:8080/groups/group_r1
```

---

## Design

### Overlap Detection — PostGIS `ST_DWithin`

Two restaurants are considered overlapping when their delivery circles intersect or touch:

```
distance(A, B) ≤ radiusA + radiusB
```

Distance is computed in metres on the spheroid using PostGIS `ST_DWithin` on a `GEOGRAPHY(POINT, 4326)` column. The geography type measures true metres (not degrees), and `ST_DWithin` is inclusive — touching circles count as overlapping. A GiST spatial index on the `location` column means PostGIS can prune distant restaurant pairs cheaply without comparing every pair.

The overlap query returns each unordered pair once (`a.id < b.id`) and excludes self-pairs:

```sql
SELECT a.id, b.id
FROM restaurants a
JOIN restaurants b ON a.id < b.id
WHERE ST_DWithin(
    a.location,
    b.location,
    CAST(a.delivery_radius_meters AS float8) + CAST(b.delivery_radius_meters AS float8)
)
```

### Transitive Grouping — Union-Find

Overlap is transitive: if A overlaps B and B overlaps C, all three belong to the same group even if A and C don't directly overlap. This is the classic connected-components problem. The service uses **Union-Find with path compression and union by rank**, giving near-O(α(n)) per operation (effectively O(1)).

Pipeline on every `POST /restaurants`:

1. Flush new restaurants to the DB (required before the native `ST_DWithin` query).
2. Seed Union-Find with every restaurant ID.
3. Run the overlap query; call `union(a, b)` for every returned pair.
4. Group restaurants by their Union-Find root.
5. Within each component, sort by restaurant ID (determinism).
6. Assign `groupId = "group_" + smallestMemberId` — stable, meaningful, not the internal UF root.
7. Compute target location and persist `delivery_groups` + `group_members`.

### Target Location — Centroid + Cover-All Radius

For each group:

```
centroid = spherical mean of member coordinates (vector averaging on unit sphere)
radius   = ceil( max over members of: haversine(centroid, member) + member.deliveryRadiusMeters )
```

Each restaurant's latitude/longitude is converted to a 3D unit-sphere vector, the vectors are averaged, and the result is converted back to lat/lon. This correctly handles the ±180° longitude boundary — two restaurants on opposite sides of the antimeridian get a centroid near ±180°, not near 0°. The radius is the smallest circle centred on the centroid that covers every restaurant's full delivery circle. Haversine is used here (pure Java, no DB round-trip) because this is a recommended covering approximation, not a precision spatial query.

A single-member group's target equals its own location and radius exactly.

### Determinism Guarantees

- Members are sorted by restaurant ID before computing the centroid (avoids floating-point sum-order sensitivity).
- `groupId` is derived from the lexicographically smallest member ID — stable across re-uploads of the same data.
- `GET /groups` orders groups by `groupId` ascending.
- Same input in any order → identical group IDs, member lists, and target values.

---

## Complexity

| Dataset | Overlap pairs (typical) | `ST_DWithin` cost | Union-Find | Total |
|---------|------------------------|-------------------|------------|-------|
| 4 000   | O(n·k), k = local density | GiST-indexed, fast | O(n·α(n)) | < 10 s measured locally in Testcontainers (native ARM64 image); meets spec target |
| 40 000  | scales with density    | GiST prunes distant pairs | near-linear | ~seconds |
| 400 000+| bottleneck: dense clusters returning many pairs | GiST helps globally but not locally | still near-linear | needs async + sharding |

The naive O(n²) complexity applies only in the degenerate case where every restaurant overlaps every other (e.g. all in the same point). In realistic data — city-scale clusters with finite delivery radii — the GiST index prunes far-away restaurants cheaply and the number of returned pairs scales with local density, not n².

**Bottleneck at scale:** the number of overlapping pairs the `ST_DWithin` query returns, not restaurant count alone. For 400 000+ restaurants, the recommended path is:
- Move grouping to an async background job triggered by the upload.
- Partition restaurants geographically (bounding-box sharding or H3 cells) and run grouping per partition with a merge step for cross-boundary overlaps.
- Use read replicas for `GET /groups` queries.

---

## Running Tests

Unit tests (no Spring context, run in milliseconds):

```bash
./mvnw test
```

Integration tests (Testcontainers spins up a real PostGIS container):

```bash
./mvnw verify
```

Test coverage:
- `UnionFindTest` — singleton, two-node union, transitive chain, idempotent re-union
- `HaversineTest` — known city-pair distance within ±1% tolerance, zero distance
- `TargetCalculatorTest` — single member, cover-all radius, order independence
- `GroupingServiceTest` — connected components, isolated restaurant forms its own group
- `EndToEndIT` — full HTTP round-trips against PostGIS: spec example (A/B/C + isolated D), determinism, full replacement, 404 for unknown group, validation errors
- `PerformanceSmokeIT` — uploads 4,000 restaurants across 5 realistic city clusters, asserts completion under 15 s (smoke ceiling), and logs the elapsed time against the < 10 s production target

---

## Production Roadmap

**What's missing for production:**

| Concern | Current state | Production path |
|---------|--------------|-----------------|
| Auth / multi-tenancy | None | API key or OAuth2 per tenant |
| Rate limiting | None | Gateway-level (Kong, AWS API GW) |
| Async grouping | Synchronous in POST transaction | Background job + `202 Accepted` + polling endpoint |
| Data versioning | Full replace only | Versioned dataset pointer; atomic swap on completion |
| Concurrent uploads | No lock | Advisory lock on the dataset version row |
| Monitoring | `/actuator/health`, `/actuator/metrics` | Prometheus scrape + Grafana dashboards for grouping latency, pair count, group count |
| Persistence | PostgreSQL (done) | Read replicas for GET queries |
| Multi-instance cache | In-process (none added) | Redis with coordinated invalidation after POST commit |

**Future enhancements:**
- **Incremental updates** — add/remove restaurants without full recomputation by maintaining the Union-Find structure and re-evaluating only the affected neighbourhood.
- **Tighter target circle** — replace centroid + cover-all with a minimum enclosing circle (Welzl's algorithm) for a smaller, more accurate target radius.
- **Geographic sharding** — partition by H3 cell or bounding box for 400 000+ restaurants; run grouping per shard with a cross-boundary merge pass.
- **Real-time updates** — event-driven re-grouping via change-data-capture on the restaurant table.

---

## Known Limitations

- **Haversine vs. spheroid** — overlap detection uses PostGIS geography (spheroid, accurate to ~0.5% over long distances); the target radius uses Haversine (sphere, ~0.3% error). Both are within acceptable tolerances for delivery-zone targeting.
- **In-memory Union-Find** — for 400 000+ restaurants the parent and rank maps consume significant heap. A disk-backed or distributed structure would be needed at that scale.
- **No pagination** — `GET /groups` returns all groups in a single response. With 400 000 restaurants forming many groups, this response could be tens of megabytes and time out downstream consumers. The fix is cursor-based pagination: add an optional `?after=<groupId>&limit=<n>` query parameter, use `WHERE group_id > :after ORDER BY group_id LIMIT :n` in the query, and return a `nextCursor` field in the response body. `GET /groups/{groupId}` is already single-resource and unaffected.

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Build | Maven (wrapper committed) |
| Database | PostgreSQL 16 + PostGIS 3.5 |
| Migrations | Flyway |
| Local DB | Docker Compose |
| Integration tests | Testcontainers + JUnit 5 + AssertJ |
| Observability | Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`) |
