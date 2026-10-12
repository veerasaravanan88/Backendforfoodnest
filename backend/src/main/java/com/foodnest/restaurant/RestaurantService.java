package com.foodnest.restaurant;

import com.foodnest.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public List<RestaurantDto> getAllActiveRestaurants(String search, String cuisine) {
        List<Restaurant> list;
        if (search != null && !search.trim().isEmpty()) {
            list = restaurantRepository.findByAccountStatusAndNameContainingIgnoreCase("ACTIVE", search.trim());
        } else if (cuisine != null && !cuisine.trim().isEmpty() && !cuisine.equalsIgnoreCase("All")) {
            list = restaurantRepository.findByAccountStatusAndCuisineTypeContainingIgnoreCase("ACTIVE", cuisine.trim());
        } else {
            list = restaurantRepository.findByAccountStatus("ACTIVE");
        }

        return list.stream()
                .map(RestaurantDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return RestaurantDto.fromEntity(restaurant);
    }

    @Transactional(readOnly = true)
    public RestaurantDto getMyProfile(Long restaurantId) {
        return getRestaurantById(restaurantId);
    }

    @Transactional
    public RestaurantDto updateProfile(Long restaurantId, UpdateRestaurantProfileRequest req) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        restaurant.setName(req.getName());
        restaurant.setOwnerName(req.getOwnerName());
        restaurant.setPhone(req.getPhone());
        restaurant.setAddress(req.getAddress());
        if (req.getCuisineType() != null) {
            restaurant.setCuisineType(req.getCuisineType());
        }
        if (req.getImageUrl() != null) {
            restaurant.setImageUrl(req.getImageUrl());
        }

        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantDto.fromEntity(saved);
    }
}
