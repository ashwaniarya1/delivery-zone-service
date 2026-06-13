package com.justeat.deliveryzone.repository;

import com.justeat.deliveryzone.domain.GroupMember;
import com.justeat.deliveryzone.domain.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    @Query("SELECT gm.id.restaurantId FROM GroupMember gm WHERE gm.id.groupId = :groupId ORDER BY gm.id.restaurantId")
    List<String> findRestaurantIdsByGroupId(@Param("groupId") String groupId);
}
