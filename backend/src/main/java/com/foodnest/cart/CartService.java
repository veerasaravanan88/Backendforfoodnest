package com.foodnest.cart;

import com.foodnest.common.BadRequestException;
import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.dish.Dish;
import com.foodnest.dish.DishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    public static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% tax
    public static final BigDecimal STANDARD_DELIVERY_FEE = new BigDecimal("40.00");

    private final DishRepository dishRepository;

    public CartService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Transactional(readOnly = true)
    public CartCalculationResponse calculateCart(List<CartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Cart cannot be empty.");
        }

        Long restaurantId = null;
        String restaurantName = null;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CartCalculationItemResponse> itemResponses = new ArrayList<>();

        for (CartItemRequest itemReq : items) {
            Dish dish = dishRepository.findById(itemReq.getDishId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + itemReq.getDishId()));

            if (restaurantId == null) {
                restaurantId = dish.getRestaurant().getId();
                restaurantName = dish.getRestaurant().getName();
            } else if (!restaurantId.equals(dish.getRestaurant().getId())) {
                throw new BadRequestException(
                        "Your cart contains dishes from multiple restaurants ('" + restaurantName +
                        "' and '" + dish.getRestaurant().getName() + "'). AdvanceFoodnest allows ordering from one restaurant at a time. Please clear cart or order separately."
                );
            }

            if (!Boolean.TRUE.equals(dish.getIsAvailable())) {
                throw new BadRequestException("Dish '" + dish.getName() + "' is currently unavailable.");
            }

            if (dish.getAvailableStock() < itemReq.getQuantity()) {
                throw new BadRequestException("Requested quantity for '" + dish.getName() + "' exceeds available stock (" + dish.getAvailableStock() + ").");
            }

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            itemResponses.add(new CartCalculationItemResponse(
                    dish.getId(),
                    dish.getName(),
                    unitPrice,
                    itemReq.getQuantity(),
                    itemTotal,
                    dish.getImageUrl(),
                    dish.getIsAvailable()
            ));
        }

        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryFee = STANDARD_DELIVERY_FEE;
        BigDecimal totalAmount = subtotal.add(taxAmount).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        CartCalculationResponse response = new CartCalculationResponse();
        response.setRestaurantId(restaurantId);
        response.setRestaurantName(restaurantName);
        response.setItems(itemResponses);
        response.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        response.setTaxAmount(taxAmount);
        response.setDeliveryFee(deliveryFee);
        response.setTotalAmount(totalAmount);
        response.setIsValid(true);
        response.setMessage("Cart calculated successfully");

        return response;
    }
}
