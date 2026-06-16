package com.justeat.deliveryzone.algorithm;

import com.justeat.deliveryzone.domain.Restaurant;
import com.justeat.deliveryzone.geo.Haversine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetCalculatorTest {

    private static Restaurant r(String id, double lat, double lon, int radius) {
        return new Restaurant(id, "Name " + id, lat, lon, radius);
    }

    @Test
    void single_restaurant_target_equals_its_own_location_and_radius() {
        Restaurant a = r("r1", 51.5074, -0.1278, 3000);

        TargetCalculator.Target target = TargetCalculator.calculate(List.of(a));

        assertThat(target.latitude()).isCloseTo(51.5074, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(target.longitude()).isCloseTo(-0.1278, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(target.radiusMeters()).isEqualTo(3000);
    }

    @Test
    void radius_covers_farthest_restaurant_delivery_circle() {
        Restaurant a = r("r1", 51.5, -0.1, 1000);
        Restaurant b = r("r2", 51.6, -0.1, 2000);
        List<Restaurant> members = List.of(a, b);

        TargetCalculator.Target target = TargetCalculator.calculate(members);

        double distA = Haversine.distanceMeters(target.latitude(), target.longitude(), 51.5, -0.1);
        double distB = Haversine.distanceMeters(target.latitude(), target.longitude(), 51.6, -0.1);

        assertThat((double) target.radiusMeters()).isGreaterThanOrEqualTo(distA + 1000);
        assertThat((double) target.radiusMeters()).isGreaterThanOrEqualTo(distB + 2000);
    }

    @Test
    void order_independence_different_input_orderings_produce_identical_target() {
        Restaurant a = r("r1", 51.5074, -0.1278, 3000);
        Restaurant b = r("r2", 51.5100, -0.1300, 2500);
        Restaurant c = r("r3", 51.5090, -0.1250, 2000);

        TargetCalculator.Target expected = TargetCalculator.calculate(List.of(a, b, c));

        for (List<Restaurant> permutation : List.of(
                List.of(c, b, a),
                List.of(b, a, c),
                List.of(c, a, b))) {
            TargetCalculator.Target actual = TargetCalculator.calculate(permutation);
            assertThat(actual.latitude()).isEqualTo(expected.latitude());
            assertThat(actual.longitude()).isEqualTo(expected.longitude());
            assertThat(actual.radiusMeters()).isEqualTo(expected.radiusMeters());
        }
    }

    @Test
    void centroid_handles_antimeridian_crossing() {
        Restaurant a = r("r1", 0.0, 179.9, 1000);
        Restaurant b = r("r2", 0.0, -179.9, 1000);

        TargetCalculator.Target target = TargetCalculator.calculate(List.of(a, b));

        assertThat(Math.abs(target.longitude())).isCloseTo(180.0, org.assertj.core.data.Offset.offset(0.1));
        assertThat(target.radiusMeters()).isLessThan(20_000);
    }
}
