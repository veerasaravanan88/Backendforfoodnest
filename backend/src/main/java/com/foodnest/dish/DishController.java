package com.foodnest.dish;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant/me/dishes")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DishDto>>> getMyDishes(@AuthenticationPrincipal UserPrincipal principal) {
        List<DishDto> dishes = dishService.getDishesByRestaurant(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dishes));
    }

    @GetMapping("/{dishId}")
    public ResponseEntity<ApiResponse<DishDto>> getMyDishById(@PathVariable Long dishId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        DishDto dish = dishService.getDishByIdAndRestaurant(dishId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dish));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DishDto>> createDish(@Valid @RequestBody CreateDishRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        DishDto created = dishService.createDish(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Dish added successfully", created));
    }

    @PutMapping("/{dishId}")
    public ResponseEntity<ApiResponse<DishDto>> updateDish(@PathVariable Long dishId,
                                                           @Valid @RequestBody UpdateDishRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        DishDto updated = dishService.updateDish(principal.getId(), dishId, request);
        return ResponseEntity.ok(ApiResponse.success("Dish updated successfully", updated));
    }

    @DeleteMapping("/{dishId}")
    public ResponseEntity<ApiResponse<Void>> deleteDish(@PathVariable Long dishId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        dishService.deleteDish(principal.getId(), dishId);
        return ResponseEntity.ok(ApiResponse.success("Dish deleted successfully", null));
    }
}
