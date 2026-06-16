package com.justeat.deliveryzone.api;

import com.justeat.deliveryzone.api.dto.RestaurantRequest;
import com.justeat.deliveryzone.api.dto.UploadResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PerformanceSmokeIT {

    private static final Logger log = LoggerFactory.getLogger(PerformanceSmokeIT.class);
    private static final int RESTAURANT_COUNT = 4_000;
    private static final long SMOKE_THRESHOLD_MS = 15_000;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("imresamu/postgis:16-3.5")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate restTemplate;

    // Clustered data creates local overlaps without degenerating into all-pairs overlap.
    private static final double[][] CLUSTER_CENTRES = {
            {51.5074, -0.1278},
            {53.4808, -2.2426},
            {53.8008, -1.5491},
            {52.4862, -1.8904},
            {55.8642, -4.2518},
    };

    @Test
    void uploads_4000_restaurants_under_smoke_threshold() {
        List<RestaurantRequest> restaurants = generate(RESTAURANT_COUNT);

        restTemplate.getForEntity("/actuator/health", String.class);
        long start = System.currentTimeMillis();
        ResponseEntity<UploadResponse> response =
                restTemplate.postForEntity("/restaurants", restaurants, UploadResponse.class);
        long elapsedMs = System.currentTimeMillis() - start;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UploadResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.restaurantsLoaded()).isEqualTo(RESTAURANT_COUNT);
        assertThat(body.groupCount()).isGreaterThan(0);

        log.info("Uploaded {} restaurants into {} groups in {} ms",
                RESTAURANT_COUNT, body.groupCount(), elapsedMs);

        // CI/Testcontainers is slower than the production target, so use a smoke-test ceiling.
        assertThat(elapsedMs)
                .as("POST /restaurants with %d restaurants", RESTAURANT_COUNT)
                .isLessThan(SMOKE_THRESHOLD_MS);
    }

    private static List<RestaurantRequest> generate(int count) {
        Random rng = new Random(42);
        List<RestaurantRequest> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double[] centre = CLUSTER_CENTRES[i % CLUSTER_CENTRES.length];
            double lat = centre[0] + (rng.nextDouble() - 0.5) * 0.30;
            double lon = centre[1] + (rng.nextDouble() - 0.5) * 0.30;
            int radius = 500 + rng.nextInt(2_500);
            list.add(new RestaurantRequest("perf_" + i, "Restaurant " + i, lat, lon, radius));
        }
        return list;
    }
}
