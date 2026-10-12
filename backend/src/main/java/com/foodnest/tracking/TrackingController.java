package com.foodnest.tracking;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/deliveryman/me/location")
    public ResponseEntity<ApiResponse<LocationResponse>> updateRiderLocation(
            @Valid @RequestBody LocationUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationResponse response = trackingService.saveLocation(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", response));
    }

    @GetMapping("/tracking/orders/{orderId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getOrderTracking(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocationResponse response = trackingService.getTrackingForOrder(orderId, principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
