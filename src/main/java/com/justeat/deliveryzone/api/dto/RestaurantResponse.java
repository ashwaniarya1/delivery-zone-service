package com.justeat.deliveryzone.api.dto;

public record RestaurantResponse(
        String id,
        String name,
        double latitude,
        double longitude,
        int deliveryRadiusMeters
) {}
