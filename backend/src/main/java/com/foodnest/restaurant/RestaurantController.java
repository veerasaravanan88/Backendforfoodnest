package com.foodnest.restaurant;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant/me")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RestaurantDto>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        RestaurantDto profile = restaurantService.getMyProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<RestaurantDto>> updateMyProfile(
            @Valid @RequestBody UpdateRestaurantProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RestaurantDto updated = restaurantService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}
