package com.justeat.deliveryzone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_groups")
public class DeliveryGroup {

    @Id
    @Column(name = "group_id")
    private String groupId;

    @Column(name = "restaurant_count", nullable = false)
    private int restaurantCount;

    @Column(name = "target_latitude", nullable = false)
    private double targetLatitude;

    @Column(name = "target_longitude", nullable = false)
    private double targetLongitude;

    @Column(name = "target_radius_meters", nullable = false)
    private int targetRadiusMeters;

    protected DeliveryGroup() {}

    public DeliveryGroup(String groupId, int restaurantCount,
                         double targetLatitude, double targetLongitude, int targetRadiusMeters) {
        this.groupId = groupId;
        this.restaurantCount = restaurantCount;
        this.targetLatitude = targetLatitude;
        this.targetLongitude = targetLongitude;
        this.targetRadiusMeters = targetRadiusMeters;
    }

    public String getGroupId() { return groupId; }
    public int getRestaurantCount() { return restaurantCount; }
    public double getTargetLatitude() { return targetLatitude; }
    public double getTargetLongitude() { return targetLongitude; }
    public int getTargetRadiusMeters() { return targetRadiusMeters; }
}
