package com.foodnest.restaurant;

import com.foodnest.common.ApiResponse;
import com.foodnest.dish.DishDto;
import com.foodnest.dish.DishService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantPublicController {

    private final RestaurantService restaurantService;
    private final DishService dishService;

    public RestaurantPublicController(RestaurantService restaurantService, DishService dishService) {
        this.restaurantService = restaurantService;
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantDto>>> getAllRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cuisine) {
        List<RestaurantDto> restaurants = restaurantService.getAllActiveRestaurants(search, cuisine);
        return ResponseEntity.ok(ApiResponse.success(restaurants));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantById(@PathVariable Long restaurantId) {
        RestaurantDto restaurant = restaurantService.getRestaurantById(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(restaurant));
    }

    @GetMapping("/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<DishDto>>> getRestaurantMenu(@PathVariable Long restaurantId) {
        List<DishDto> menu = dishService.getAvailableDishesByRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(menu));
    }
}
