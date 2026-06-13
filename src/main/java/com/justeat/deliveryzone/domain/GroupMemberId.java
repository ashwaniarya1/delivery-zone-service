package com.justeat.deliveryzone.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GroupMemberId implements Serializable {

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "restaurant_id")
    private String restaurantId;

    protected GroupMemberId() {}

    public GroupMemberId(String groupId, String restaurantId) {
        this.groupId = groupId;
        this.restaurantId = restaurantId;
    }

    public String getGroupId() { return groupId; }
    public String getRestaurantId() { return restaurantId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMemberId that)) return false;
        return Objects.equals(groupId, that.groupId) && Objects.equals(restaurantId, that.restaurantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, restaurantId);
    }
}
