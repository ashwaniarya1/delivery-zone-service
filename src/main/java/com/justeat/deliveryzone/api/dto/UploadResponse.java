package com.justeat.deliveryzone.api.dto;

public record UploadResponse(
        String status,
        int restaurantsLoaded,
        String message
) {}
