package com.foodnest.customer;

import com.foodnest.common.ApiResponse;
import com.foodnest.order.OrderDto;
import com.foodnest.order.OrderService;
import com.foodnest.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/me")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    public CustomerController(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileDto>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileDto profile = customerService.getProfileByPrincipal(principal);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CustomerProfileDto>> updateProfile(
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileDto updated = customerService.updateProfileByPrincipal(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        List<OrderDto> orders = orderService.getCustomerOrdersByPrincipal(principal);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderDto>> getMyOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.getOrderByIdForAuthorizedUser(orderId, principal);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
