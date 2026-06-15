package com.justeat.deliveryzone.algorithm;

import com.justeat.deliveryzone.domain.Restaurant;
import com.justeat.deliveryzone.geo.Haversine;

import java.util.Comparator;
import java.util.List;

public final class TargetCalculator {

    private TargetCalculator() {}

    public record Target(double latitude, double longitude, int radiusMeters) {}

    public static Target calculate(List<Restaurant> members) {
        if (members.size() == 1) {
            Restaurant r = members.getFirst();
            return new Target(r.getLatitude(), r.getLongitude(), r.getDeliveryRadiusMeters());
        }

        List<Restaurant> sorted = members.stream()
                .sorted(Comparator.comparing(Restaurant::getId))
                .toList();
        double[] centroid = sphericalCentroid(sorted);
        double centroidLat = centroid[0];
        double centroidLon = centroid[1];

        int radius = sorted.stream()
                .mapToInt(r -> {
                    double dist = Haversine.distanceMeters(centroidLat, centroidLon,
                            r.getLatitude(), r.getLongitude());
                    return (int) Math.ceil(dist + r.getDeliveryRadiusMeters());
                })
                .max()
                .orElse(0);

        return new Target(centroidLat, centroidLon, radius);
    }

    private static double[] sphericalCentroid(List<Restaurant> members) {
        double x = 0, y = 0, z = 0;

        for (Restaurant r : members) {
            double lat = Math.toRadians(r.getLatitude());
            double lon = Math.toRadians(r.getLongitude());
            x += Math.cos(lat) * Math.cos(lon);
            y += Math.cos(lat) * Math.sin(lon);
            z += Math.sin(lat);
        }

        int n = members.size();
        x /= n;
        y /= n;
        z /= n;

        double lon = Math.atan2(y, x);
        double hyp = Math.sqrt(x * x + y * y);
        double lat = Math.atan2(z, hyp);

        return new double[]{Math.toDegrees(lat), Math.toDegrees(lon)};
    }
}
