package com.foodnest.delivery;

import com.foodnest.common.ApiResponse;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.order.OrderDto;
import com.foodnest.order.OrderService;
import com.foodnest.order.UpdateOrderStatusRequest;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deliveryman/me")
@Transactional(readOnly = true)
public class DeliveryController {

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final DeliveryManRepository deliveryManRepository;
    private final OrderService orderService;

    public DeliveryController(DeliveryAssignmentRepository assignmentRepository,
                              DeliveryAssignmentService deliveryAssignmentService,
                              DeliveryManRepository deliveryManRepository,
                              OrderService orderService) {
        this.assignmentRepository = assignmentRepository;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.deliveryManRepository = deliveryManRepository;
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
    @Transactional
    public ResponseEntity<ApiResponse<OrderDto>> getActiveDelivery(@AuthenticationPrincipal UserPrincipal principal) {
        // Heartbeat: update rider last active timestamp
        deliveryManRepository.findById(principal.getId()).ifPresent(rider -> {
            rider.setLastActiveAt(LocalDateTime.now());
            deliveryManRepository.save(rider);
        });

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

    @GetMapping("/available-orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAvailableOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeliveryAssignment> awaiting = assignmentRepository.findByStatus(AssignmentStatus.AWAITING_ASSIGNMENT);
        List<OrderDto> dtos = awaiting.stream()
                .map(da -> orderService.getOrderByIdForAuthorizedUser(da.getOrder().getId(), principal))
                .filter(dto -> dto != null && ("PLACED".equals(dto.getStatus()) || "ACCEPTED".equals(dto.getStatus()) || "PREPARING".equals(dto.getStatus()) || "READY".equals(dto.getStatus())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/orders/{orderId}/claim")
    @Transactional
    public ResponseEntity<ApiResponse<DeliveryAssignmentDto>> claimOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeliveryAssignment assigned = deliveryAssignmentService.claimOrder(orderId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Order accepted for delivery!", DeliveryAssignmentDto.fromEntity(assigned)));
    }

    @PutMapping("/deliveries/{orderId}/status")
    @Transactional
    public ResponseEntity<ApiResponse<OrderDto>> updateDeliveryStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto updated = orderService.updateOrderStatusByDeliveryMan(principal.getId(), orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully to " + request.getStatus(), updated));
    }
}
