package com.justeat.deliveryzone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "delivery_radius_meters", nullable = false)
    private int deliveryRadiusMeters;

    protected Restaurant() {}

    public Restaurant(String id, String name, double latitude, double longitude, int deliveryRadiusMeters) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deliveryRadiusMeters = deliveryRadiusMeters;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getDeliveryRadiusMeters() { return deliveryRadiusMeters; }
}
