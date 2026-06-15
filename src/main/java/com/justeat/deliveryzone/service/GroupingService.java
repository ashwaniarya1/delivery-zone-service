package com.justeat.deliveryzone.service;

import com.justeat.deliveryzone.algorithm.TargetCalculator;
import com.justeat.deliveryzone.algorithm.UnionFind;
import com.justeat.deliveryzone.domain.DeliveryGroup;
import com.justeat.deliveryzone.domain.GroupMember;
import com.justeat.deliveryzone.domain.Restaurant;
import com.justeat.deliveryzone.repository.DeliveryGroupRepository;
import com.justeat.deliveryzone.repository.GroupMemberRepository;
import com.justeat.deliveryzone.repository.OverlapQueryRepository;
import com.justeat.deliveryzone.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupingService {

    private final RestaurantRepository restaurantRepository;
    private final OverlapQueryRepository overlapQueryRepository;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupingService(RestaurantRepository restaurantRepository,
                           OverlapQueryRepository overlapQueryRepository,
                           DeliveryGroupRepository deliveryGroupRepository,
                           GroupMemberRepository groupMemberRepository) {
        this.restaurantRepository = restaurantRepository;
        this.overlapQueryRepository = overlapQueryRepository;
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public int recomputeGroups() {
        // group_members are already cascade-deleted when the restaurants table is cleared
        deliveryGroupRepository.deleteAllInBatch();

        List<Restaurant> allRestaurants = restaurantRepository.findAll();
        if (allRestaurants.isEmpty()) return 0;

        UnionFind uf = new UnionFind();
        allRestaurants.forEach(r -> uf.add(r.getId()));

        overlapQueryRepository.processOverlappingPairs(uf::union);

        Map<String, List<Restaurant>> components = new HashMap<>();
        allRestaurants.forEach(r ->
                components.computeIfAbsent(uf.find(r.getId()), k -> new ArrayList<>()).add(r));

        List<DeliveryGroup> groups = new ArrayList<>();
        List<GroupMember> members = new ArrayList<>();

        components.values().forEach(restaurantList -> {
            restaurantList.sort(Comparator.comparing(Restaurant::getId));
            String groupId = "group_" + restaurantList.getFirst().getId();

            TargetCalculator.Target target = TargetCalculator.calculate(restaurantList);
            groups.add(new DeliveryGroup(groupId, restaurantList.size(),
                    target.latitude(), target.longitude(), target.radiusMeters()));

            restaurantList.forEach(r -> members.add(new GroupMember(groupId, r.getId())));
        });

        deliveryGroupRepository.saveAll(groups);
        groupMemberRepository.saveAll(members);

        return groups.size();
    }
}
