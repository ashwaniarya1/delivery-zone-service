package com.justeat.deliveryzone.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_members")
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    protected GroupMember() {}

    public GroupMember(String groupId, String restaurantId) {
        this.id = new GroupMemberId(groupId, restaurantId);
    }

    public GroupMemberId getId() { return id; }
    public String getGroupId() { return id.getGroupId(); }
    public String getRestaurantId() { return id.getRestaurantId(); }
}
