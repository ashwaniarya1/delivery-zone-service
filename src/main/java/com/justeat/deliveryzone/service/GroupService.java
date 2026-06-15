package com.justeat.deliveryzone.service;

import com.justeat.deliveryzone.api.dto.GroupDetailResponse;
import com.justeat.deliveryzone.api.dto.GroupSummaryResponse;
import com.justeat.deliveryzone.api.dto.GroupsResponse;
import com.justeat.deliveryzone.api.dto.RestaurantResponse;
import com.justeat.deliveryzone.api.exception.ResourceNotFoundException;
import com.justeat.deliveryzone.domain.DeliveryGroup;
import com.justeat.deliveryzone.domain.Restaurant;
import com.justeat.deliveryzone.repository.DeliveryGroupRepository;
import com.justeat.deliveryzone.repository.GroupMemberRepository;
import com.justeat.deliveryzone.repository.DeliveryGroupRepository.GroupWithMemberRow;
import com.justeat.deliveryzone.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private final DeliveryGroupRepository deliveryGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RestaurantRepository restaurantRepository;

    public GroupService(DeliveryGroupRepository deliveryGroupRepository,
                        GroupMemberRepository groupMemberRepository,
                        RestaurantRepository restaurantRepository) {
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public GroupsResponse getAllGroups() {
        List<GroupWithMemberRow> rows = deliveryGroupRepository.findAllGroupsWithRestaurantIds();
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No restaurant data has been loaded yet");
        }

        // LinkedHashMap preserves the ORDER BY group_id ordering from the query.
        Map<String, GroupWithMemberRow> groupMeta = new LinkedHashMap<>();
        Map<String, List<String>> memberIds = new LinkedHashMap<>();

        for (GroupWithMemberRow row : rows) {
            String groupId = row.getGroupId();
            groupMeta.putIfAbsent(groupId, row);
            if (row.getRestaurantId() != null) {
                memberIds.computeIfAbsent(groupId, k -> new ArrayList<>()).add(row.getRestaurantId());
            }
        }

        List<GroupSummaryResponse> summaries = groupMeta.entrySet().stream()
                .map(e -> {
                    String gid = e.getKey();
                    GroupWithMemberRow meta = e.getValue();
                    return new GroupSummaryResponse(
                            gid,
                            meta.getRestaurantCount(),
                            memberIds.getOrDefault(gid, List.of()),
                            meta.getTargetLatitude(),
                            meta.getTargetLongitude(),
                            meta.getTargetRadiusMeters());
                })
                .toList();

        return new GroupsResponse(summaries.size(), summaries);
    }

    public GroupDetailResponse getGroup(String groupId) {
        DeliveryGroup group = deliveryGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        List<String> restaurantIds = groupMemberRepository.findRestaurantIdsByGroupId(groupId);

        List<Restaurant> restaurants = restaurantRepository.findAllById(List.copyOf(restaurantIds));
        restaurants.sort(Comparator.comparing(Restaurant::getId));

        List<RestaurantResponse> restaurantResponses = restaurants.stream()
                .map(r -> new RestaurantResponse(r.getId(), r.getName(),
                        r.getLatitude(), r.getLongitude(), r.getDeliveryRadiusMeters()))
                .toList();

        return new GroupDetailResponse(
                group.getGroupId(),
                group.getRestaurantCount(),
                restaurantIds,
                group.getTargetLatitude(),
                group.getTargetLongitude(),
                group.getTargetRadiusMeters(),
                restaurantResponses);
    }
}
