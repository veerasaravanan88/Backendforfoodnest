package com.foodnest.delivery;

import com.foodnest.common.ApiResponse;
import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.order.OrderDto;
import com.foodnest.order.OrderService;
import com.foodnest.order.UpdateOrderStatusRequest;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deliveryman/me")
public class DeliveryController {

    private final DeliveryAssignmentRepository assignmentRepository;
    private final OrderService orderService;

    public DeliveryController(DeliveryAssignmentRepository assignmentRepository, OrderService orderService) {
        this.assignmentRepository = assignmentRepository;
        this.orderService = orderService;
    }

    @GetMapping("/deliveries")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDto>>> getMyDeliveries(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeliveryAssignmentDto> list = assignmentRepository.findByDeliveryManIdOrderByAssignedAtDesc(principal.getId())
                .stream()
                .map(DeliveryAssignmentDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentDto>>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return getMyDeliveries(principal);
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<OrderDto>> getActiveDelivery(@AuthenticationPrincipal UserPrincipal principal) {
        return assignmentRepository.findFirstByDeliveryManIdAndStatusInOrderByAssignedAtDesc(
                        principal.getId(),
                        Arrays.asList(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP)
                )
                .map(assignment -> {
                    OrderDto dto = orderService.getOrderByIdForAuthorizedUser(assignment.getOrder().getId(), principal);
                    return ResponseEntity.ok(ApiResponse.success(dto));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("No active delivery in progress", null)));
    }

    @PutMapping("/deliveries/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateDeliveryStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto updated = orderService.updateOrderStatusByDeliveryMan(principal.getId(), orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully to " + request.getStatus(), updated));
    }
}
