package com.foodnest.dish;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByRestaurantId(Long restaurantId);
    List<Dish> findByRestaurantIdAndIsAvailableTrue(Long restaurantId);
    List<Dish> findByRestaurantIdAndCategory(Long restaurantId, String category);
    Optional<Dish> findByIdAndRestaurantId(Long id, Long restaurantId);
    boolean existsByIdAndRestaurantId(Long id, Long restaurantId);
}
