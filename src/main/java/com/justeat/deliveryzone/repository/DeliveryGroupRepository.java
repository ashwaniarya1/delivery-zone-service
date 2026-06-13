package com.justeat.deliveryzone.repository;

import com.justeat.deliveryzone.domain.DeliveryGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryGroupRepository extends JpaRepository<DeliveryGroup, String> {

    List<DeliveryGroup> findAllByOrderByGroupIdAsc();
}
