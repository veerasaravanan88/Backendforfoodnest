package com.foodnest.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CartCalculationRequest {

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CartItemRequest> items;

    public CartCalculationRequest() {
    }

    public CartCalculationRequest(List<CartItemRequest> items) {
        this.items = items;
    }

    public List<CartItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CartItemRequest> items) {
        this.items = items;
    }
}
