package com.justeat.deliveryzone.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.util.function.BiConsumer;

@Repository
public class OverlapQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public OverlapQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Stream overlap pairs so large result sets are not materialized in memory.
    public void processOverlappingPairs(BiConsumer<String, String> pairConsumer) {
        String sql = """
                SELECT a.id, b.id
                FROM restaurants a
                JOIN restaurants b ON a.id < b.id
                WHERE ST_DWithin(
                    a.location,
                    b.location,
                    CAST(a.delivery_radius_meters AS float8) + CAST(b.delivery_radius_meters AS float8)
                )
                """;

        jdbcTemplate.query(sql, (RowCallbackHandler) rs ->
                pairConsumer.accept(rs.getString(1), rs.getString(2)));
    }
}
