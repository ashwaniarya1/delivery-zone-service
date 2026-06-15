package com.justeat.deliveryzone.api.dto;

import java.util.List;

public record GroupsResponse(
        int groupCount,
        List<GroupSummaryResponse> groups
) {}
