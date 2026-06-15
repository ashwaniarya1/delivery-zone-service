package com.justeat.deliveryzone.repository;

import com.justeat.deliveryzone.domain.DeliveryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeliveryGroupRepository extends JpaRepository<DeliveryGroup, String> {

    interface GroupWithMemberRow {
        String getGroupId();
        int getRestaurantCount();
        double getTargetLatitude();
        double getTargetLongitude();
        int getTargetRadiusMeters();
        String getRestaurantId();
    }

    @Query(value = """
            SELECT g.group_id, g.restaurant_count, g.target_latitude, g.target_longitude,
                   g.target_radius_meters, gm.restaurant_id
            FROM delivery_groups g
            LEFT JOIN group_members gm ON gm.group_id = g.group_id
            ORDER BY g.group_id, gm.restaurant_id
            """, nativeQuery = true)
    List<GroupWithMemberRow> findAllGroupsWithRestaurantIds();
}
