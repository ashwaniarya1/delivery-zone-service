package com.justeat.deliveryzone.repository;

import com.justeat.deliveryzone.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

    @Query("SELECT r.id FROM Restaurant r ORDER BY r.id")
    List<String> findAllIds();
}
