package com.justeat.deliveryzone.service;

import com.justeat.deliveryzone.domain.DeliveryGroup;
import com.justeat.deliveryzone.domain.GroupMember;
import com.justeat.deliveryzone.domain.Restaurant;
import com.justeat.deliveryzone.repository.DeliveryGroupRepository;
import com.justeat.deliveryzone.repository.GroupMemberRepository;
import com.justeat.deliveryzone.repository.OverlapQueryRepository;
import com.justeat.deliveryzone.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupingServiceTest {

    @Mock RestaurantRepository restaurantRepository;
    @Mock OverlapQueryRepository overlapQueryRepository;
    @Mock DeliveryGroupRepository deliveryGroupRepository;
    @Mock GroupMemberRepository groupMemberRepository;

    @InjectMocks GroupingService groupingService;

    @Captor ArgumentCaptor<List<DeliveryGroup>> groupCaptor;
    @Captor ArgumentCaptor<List<GroupMember>> memberCaptor;

    private static Restaurant r(String id, double lat, double lon, int radius) {
        return new Restaurant(id, "Name " + id, lat, lon, radius);
    }

    @SafeVarargs
    private void stubPairs(String[]... pairs) {
        doAnswer(inv -> {
            BiConsumer<String, String> consumer = inv.getArgument(0);
            for (String[] pair : pairs) {
                consumer.accept(pair[0], pair[1]);
            }
            return null;
        }).when(overlapQueryRepository).processOverlappingPairs(any());
    }

    @Test
    void no_restaurants_returns_zero_groups() {
        when(restaurantRepository.findAll()).thenReturn(List.of());

        assertThat(groupingService.recomputeGroups()).isEqualTo(0);
    }

    @Test
    void isolated_restaurant_forms_its_own_group() {
        Restaurant d = r("r4", 51.6, -0.3, 1500);
        stubPairs();
        when(restaurantRepository.findAll()).thenReturn(List.of(d));

        assertThat(groupingService.recomputeGroups()).isEqualTo(1);

        verify(deliveryGroupRepository).saveAll(groupCaptor.capture());
        DeliveryGroup group = groupCaptor.getValue().get(0);
        assertThat(group.getGroupId()).isEqualTo("group_r4");
        assertThat(group.getRestaurantCount()).isEqualTo(1);
    }

    @Test
    void direct_overlap_puts_two_restaurants_in_same_group() {
        Restaurant a = r("r1", 51.5074, -0.1278, 3000);
        Restaurant b = r("r2", 51.5100, -0.1300, 2500);
        stubPairs(new String[]{"r1", "r2"});
        when(restaurantRepository.findAll()).thenReturn(List.of(a, b));

        assertThat(groupingService.recomputeGroups()).isEqualTo(1);

        verify(deliveryGroupRepository).saveAll(groupCaptor.capture());
        DeliveryGroup group = groupCaptor.getValue().get(0);
        assertThat(group.getGroupId()).isEqualTo("group_r1");
        assertThat(group.getRestaurantCount()).isEqualTo(2);
    }

    @Test
    void transitive_overlap_joins_three_into_one_group() {
        Restaurant a = r("r1", 51.5074, -0.1278, 3000);
        Restaurant b = r("r2", 51.5100, -0.1300, 2500);
        Restaurant c = r("r3", 51.5090, -0.1250, 2000);
        stubPairs(new String[]{"r1", "r2"}, new String[]{"r2", "r3"});
        when(restaurantRepository.findAll()).thenReturn(List.of(a, b, c));

        assertThat(groupingService.recomputeGroups()).isEqualTo(1);

        verify(deliveryGroupRepository).saveAll(groupCaptor.capture());
        DeliveryGroup group = groupCaptor.getValue().get(0);
        assertThat(group.getGroupId()).isEqualTo("group_r1");
        assertThat(group.getRestaurantCount()).isEqualTo(3);
    }

    @Test
    void mixed_overlap_and_isolated_creates_two_groups() {
        Restaurant a = r("r1", 51.5074, -0.1278, 3000);
        Restaurant b = r("r2", 51.5100, -0.1300, 2500);
        Restaurant c = r("r3", 51.5090, -0.1250, 2000);
        Restaurant d = r("r4", 51.6000, -0.3000, 1500);
        stubPairs(new String[]{"r1", "r2"}, new String[]{"r2", "r3"});
        when(restaurantRepository.findAll()).thenReturn(List.of(a, b, c, d));

        assertThat(groupingService.recomputeGroups()).isEqualTo(2);

        verify(deliveryGroupRepository).saveAll(groupCaptor.capture());
        List<String> groupIds = groupCaptor.getValue().stream()
                .map(DeliveryGroup::getGroupId).sorted().toList();
        assertThat(groupIds).containsExactly("group_r1", "group_r4");

        verify(groupMemberRepository).saveAll(memberCaptor.capture());
        assertThat(memberCaptor.getValue()).hasSize(4);
    }

    @Test
    void group_id_uses_smallest_restaurant_id_not_union_find_root() {
        Restaurant a = r("r1", 51.5, -0.1, 1000);
        Restaurant c = r("r3", 51.5, -0.1, 1000);
        stubPairs(new String[]{"r1", "r3"});
        when(restaurantRepository.findAll()).thenReturn(List.of(a, c));

        groupingService.recomputeGroups();

        verify(deliveryGroupRepository).saveAll(groupCaptor.capture());
        assertThat(groupCaptor.getValue().get(0).getGroupId()).isEqualTo("group_r1");
    }
}
