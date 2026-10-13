package com.foodnest.cart;

import com.foodnest.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CartCalculationResponse>> calculateCart(
            @Valid @RequestBody CartCalculationRequest request) {
        CartCalculationResponse response = cartService.calculateCart(request.getItems());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
