package com.justeat.deliveryzone.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineTest {

    private static final double TOLERANCE_METERS = 500.0;

    @Test
    void same_point_returns_zero() {
        double dist = Haversine.distanceMeters(51.5074, -0.1278, 51.5074, -0.1278);
        assertThat(dist).isEqualTo(0.0);
    }

    @Test
    void london_to_paris_is_approximately_341km() {
        double dist = Haversine.distanceMeters(51.5074, -0.1278, 48.8566, 2.3522);
        assertThat(dist).isBetween(340_000.0, 346_000.0);
    }

    @Test
    void distance_is_symmetric() {
        double forward = Haversine.distanceMeters(51.5074, -0.1278, 48.8566, 2.3522);
        double backward = Haversine.distanceMeters(48.8566, 2.3522, 51.5074, -0.1278);
        assertThat(forward).isCloseTo(backward, org.assertj.core.data.Offset.offset(TOLERANCE_METERS));
    }

    @Test
    void nearby_restaurants_return_small_distance() {
        double dist = Haversine.distanceMeters(51.5074, -0.1278, 51.5100, -0.1300);
        assertThat(dist).isBetween(200.0, 500.0);
    }
}
