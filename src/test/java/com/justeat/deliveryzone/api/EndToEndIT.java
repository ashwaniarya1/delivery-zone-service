package com.justeat.deliveryzone.api;

import com.justeat.deliveryzone.api.dto.GroupDetailResponse;
import com.justeat.deliveryzone.api.dto.GroupSummaryResponse;
import com.justeat.deliveryzone.api.dto.GroupsResponse;
import com.justeat.deliveryzone.api.dto.RestaurantRequest;
import com.justeat.deliveryzone.api.dto.UploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EndToEndIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("imresamu/postgis:16-3.5")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate restTemplate;

    static final List<RestaurantRequest> SPEC_RESTAURANTS = List.of(
            new RestaurantRequest("r1", "Restaurant A", 51.5074, -0.1278, 3000),
            new RestaurantRequest("r2", "Restaurant B", 51.5100, -0.1300, 2500),
            new RestaurantRequest("r3", "Restaurant C", 51.5090, -0.1250, 2000),
            new RestaurantRequest("r4", "Restaurant D", 51.6000, -0.3000, 1500)
    );

    private ResponseEntity<UploadResponse> lastUploadResponse;

    @BeforeEach
    void loadSpecRestaurants() {
        lastUploadResponse = restTemplate.postForEntity("/restaurants", SPEC_RESTAURANTS, UploadResponse.class);
    }

    @Test
    void post_returns_restaurant_and_group_counts() {
        assertThat(lastUploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UploadResponse body = Objects.requireNonNull(lastUploadResponse.getBody());
        assertThat(body.status()).isEqualTo("success");
        assertThat(body.restaurantsLoaded()).isEqualTo(4);
        assertThat(body.groupCount()).isEqualTo(2);
    }

    @Test
    void get_groups_returns_two_groups_with_correct_members() {
        ResponseEntity<GroupsResponse> response =
                restTemplate.getForEntity("/groups", GroupsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        GroupsResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.groupCount()).isEqualTo(2);
        assertThat(body.groups()).hasSize(2);

        GroupSummaryResponse connected = body.groups().get(0);
        assertThat(connected.groupId()).isEqualTo("group_r1");
        assertThat(connected.restaurantCount()).isEqualTo(3);
        assertThat(connected.restaurantIds()).containsExactly("r1", "r2", "r3");
        assertThat(connected.recommendedTargetRadiusMeters()).isGreaterThan(0);

        GroupSummaryResponse isolated = body.groups().get(1);
        assertThat(isolated.groupId()).isEqualTo("group_r4");
        assertThat(isolated.restaurantCount()).isEqualTo(1);
        assertThat(isolated.restaurantIds()).containsExactly("r4");
        assertThat(isolated.recommendedTargetLatitude()).isEqualTo(51.6000);
        assertThat(isolated.recommendedTargetLongitude()).isEqualTo(-0.3000);
        assertThat(isolated.recommendedTargetRadiusMeters()).isEqualTo(1500);
    }

    @Test
    void get_group_detail_returns_full_restaurant_objects() {
        ResponseEntity<GroupDetailResponse> response =
                restTemplate.getForEntity("/groups/group_r1", GroupDetailResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        GroupDetailResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.groupId()).isEqualTo("group_r1");
        assertThat(body.restaurantCount()).isEqualTo(3);
        assertThat(body.restaurantIds()).containsExactly("r1", "r2", "r3");
        assertThat(body.restaurants()).hasSize(3);

        List<String> ids = body.restaurants().stream().map(r -> r.id()).toList();
        assertThat(ids).containsExactly("r1", "r2", "r3");

        assertThat(body.recommendedTargetRadiusMeters()).isGreaterThanOrEqualTo(3000);
    }

    @Test
    void get_group_with_unknown_id_returns_404() {
        ResponseEntity<Void> response =
                restTemplate.getForEntity("/groups/group_does_not_exist", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void get_groups_after_empty_post_returns_404() {
        restTemplate.postForEntity("/restaurants", List.of(), UploadResponse.class);

        ResponseEntity<Void> response = restTemplate.getForEntity("/groups", Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void determinism_same_restaurants_different_order_produce_identical_groups() {
        GroupsResponse first = Objects.requireNonNull(
                restTemplate.getForEntity("/groups", GroupsResponse.class).getBody());

        List<RestaurantRequest> reversed = new ArrayList<>(SPEC_RESTAURANTS);
        Collections.reverse(reversed);
        restTemplate.postForEntity("/restaurants", reversed, UploadResponse.class);

        GroupsResponse second = Objects.requireNonNull(
                restTemplate.getForEntity("/groups", GroupsResponse.class).getBody());

        assertThat(second.groups())
                .usingRecursiveComparison()
                .isEqualTo(first.groups());
    }

    @Test
    void re_post_different_data_fully_replaces_previous_groups() {
        restTemplate.postForEntity("/restaurants",
                List.of(new RestaurantRequest("r4", "Restaurant D", 51.6000, -0.3000, 1500)),
                UploadResponse.class);

        GroupsResponse body = Objects.requireNonNull(
                restTemplate.getForEntity("/groups", GroupsResponse.class).getBody());
        assertThat(body.groupCount()).isEqualTo(1);
        assertThat(body.groups().get(0).groupId()).isEqualTo("group_r4");

        ResponseEntity<Void> oldGroup =
                restTemplate.getForEntity("/groups/group_r1", Void.class);
        assertThat(oldGroup.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void post_with_duplicate_ids_returns_400() {
        List<RestaurantRequest> withDupe = List.of(
                new RestaurantRequest("r1", "A", 51.5, -0.1, 1000),
                new RestaurantRequest("r1", "A duplicate", 51.5, -0.1, 1000)
        );

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/restaurants", withDupe, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void post_with_malformed_json_returns_400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("[{bad json", headers);

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/restaurants", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void post_with_invalid_latitude_returns_400() {
        List<RestaurantRequest> invalid = List.of(
                new RestaurantRequest("r1", "A", 999.0, -0.1, 1000)
        );

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/restaurants", invalid, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void post_with_null_body_returns_400_not_500() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("null", headers);

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/restaurants", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void post_with_null_element_in_array_returns_400_not_500() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("[null]", headers);

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/restaurants", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
