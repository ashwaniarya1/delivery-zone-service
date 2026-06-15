package com.justeat.deliveryzone.api.controller;

import com.justeat.deliveryzone.api.dto.RestaurantRequest;
import com.justeat.deliveryzone.api.dto.UploadResponse;
import com.justeat.deliveryzone.service.RestaurantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping("/restaurants")
    public ResponseEntity<UploadResponse> uploadRestaurants(
            @RequestBody @NotNull @Valid List<@NotNull @Valid RestaurantRequest> restaurants) {
        return ResponseEntity.ok(restaurantService.replaceAll(restaurants));
    }
}
