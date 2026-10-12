package com.foodnest.order;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Customer places order
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully!", order));
    }

    // Single order details for authorized user
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.getOrderByIdForAuthorizedUser(orderId, principal);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // Customer cancels order
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.cancelOrderByCustomer(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully.", order));
    }

    // Restaurant views orders
    @GetMapping("/restaurant/me/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getRestaurantOrders(
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<OrderDto> orders = orderService.getRestaurantOrders(principal.getId(), status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // Restaurant views single order
    @GetMapping("/restaurant/me/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getRestaurantOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.getRestaurantOrderById(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // Restaurant updates order status (ACCEPTED, PREPARING, READY)
    @PutMapping("/restaurant/me/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatusByRestaurant(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.updateOrderStatusByRestaurant(principal.getId(), orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully to " + request.getStatus(), order));
    }
}
