package com.justeat.deliveryzone.api.dto;

import java.util.List;

public record GroupSummaryResponse(
        String groupId,
        int restaurantCount,
        List<String> restaurantIds,
        double recommendedTargetLatitude,
        double recommendedTargetLongitude,
        int recommendedTargetRadiusMeters
) {}
