package com.justeat.deliveryzone.repository;

import com.justeat.deliveryzone.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, String> {
}
