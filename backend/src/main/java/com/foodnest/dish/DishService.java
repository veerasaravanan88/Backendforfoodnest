package com.foodnest.dish;

import com.foodnest.common.ForbiddenException;
import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.restaurant.Restaurant;
import com.foodnest.restaurant.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final RestaurantRepository restaurantRepository;

    public DishService(DishRepository dishRepository, RestaurantRepository restaurantRepository) {
        this.dishRepository = dishRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public List<DishDto> getDishesByRestaurant(Long restaurantId) {
        return dishRepository.findByRestaurantId(restaurantId).stream()
                .map(DishDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DishDto> getAvailableDishesByRestaurant(Long restaurantId) {
        return dishRepository.findByRestaurantIdAndIsAvailableTrue(restaurantId).stream()
                .map(DishDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DishDto getDishByIdAndRestaurant(Long dishId, Long restaurantId) {
        Dish dish = dishRepository.findByIdAndRestaurantId(dishId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found or does not belong to your restaurant"));
        return DishDto.fromEntity(dish);
    }

    @Transactional
    public DishDto createDish(Long restaurantId, CreateDishRequest req) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        Dish dish = new Dish();
        dish.setRestaurant(restaurant);
        dish.setName(req.getName());
        dish.setDescription(req.getDescription());
        dish.setCategory(req.getCategory());
        dish.setPrice(req.getPrice());
        dish.setAvailableStock(req.getAvailableStock() != null ? req.getAvailableStock() : 100);
        dish.setImageUrl(req.getImageUrl());
        dish.setIsAvailable(req.getIsAvailable() != null ? req.getIsAvailable() : true);

        Dish saved = dishRepository.save(dish);
        return DishDto.fromEntity(saved);
    }

    @Transactional
    public DishDto updateDish(Long restaurantId, Long dishId, UpdateDishRequest req) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + dishId));

        if (!dish.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("Access denied: You cannot modify dishes belonging to another restaurant.");
        }

        dish.setName(req.getName());
        dish.setDescription(req.getDescription());
        dish.setCategory(req.getCategory());
        dish.setPrice(req.getPrice());
        if (req.getAvailableStock() != null) {
            dish.setAvailableStock(req.getAvailableStock());
        }
        if (req.getImageUrl() != null) {
            dish.setImageUrl(req.getImageUrl());
        }
        dish.setIsAvailable(req.getIsAvailable());

        Dish saved = dishRepository.save(dish);
        return DishDto.fromEntity(saved);
    }

    @Transactional
    public void deleteDish(Long restaurantId, Long dishId) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + dishId));

        if (!dish.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("Access denied: You cannot delete dishes belonging to another restaurant.");
        }

        dishRepository.delete(dish);
    }
}
