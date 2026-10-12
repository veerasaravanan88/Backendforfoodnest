package com.foodnest.deliveryman;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveryman/me")
public class DeliveryManController {

    private final DeliveryManService deliveryManService;

    public DeliveryManController(DeliveryManService deliveryManService) {
        this.deliveryManService = deliveryManService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DeliveryManDto>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        DeliveryManDto profile = deliveryManService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/availability")
    public ResponseEntity<ApiResponse<DeliveryManDto>> updateAvailability(
            @Valid @RequestBody AvailabilityUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryManDto updated = deliveryManService.updateAvailability(principal.getId(), request.getIsAvailable());
        return ResponseEntity.ok(ApiResponse.success("Availability updated successfully", updated));
    }
}
