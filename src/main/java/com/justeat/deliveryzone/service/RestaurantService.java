package com.justeat.deliveryzone.service;

import com.justeat.deliveryzone.api.dto.RestaurantRequest;
import com.justeat.deliveryzone.api.dto.UploadResponse;
import com.justeat.deliveryzone.domain.Restaurant;
import com.justeat.deliveryzone.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final GroupingService groupingService;

    public RestaurantService(RestaurantRepository restaurantRepository,
                             GroupingService groupingService) {
        this.restaurantRepository = restaurantRepository;
        this.groupingService = groupingService;
    }

    @Transactional
    public UploadResponse replaceAll(List<RestaurantRequest> requests) {
        validateNoDuplicateIds(requests);

        restaurantRepository.deleteAllInBatch();

        List<Restaurant> entities = requests.stream()
                .map(r -> new Restaurant(r.id(), r.name(), r.latitude(), r.longitude(), r.deliveryRadiusMeters()))
                .collect(Collectors.toCollection(ArrayList::new));

        restaurantRepository.saveAllAndFlush(entities);

        int groupCount = groupingService.recomputeGroups();

        return new UploadResponse("success", entities.size(), groupCount, "Restaurants successfully stored");
    }

    private void validateNoDuplicateIds(List<RestaurantRequest> requests) {
        Set<String> seen = new HashSet<>();
        for (RestaurantRequest r : requests) {
            if (!seen.add(r.id())) {
                throw new IllegalArgumentException("Duplicate restaurant id in payload: " + r.id());
            }
        }
    }
}
