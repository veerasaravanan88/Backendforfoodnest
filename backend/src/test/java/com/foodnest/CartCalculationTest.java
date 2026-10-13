package com.foodnest;

import com.foodnest.cart.CartCalculationResponse;
import com.foodnest.cart.CartItemRequest;
import com.foodnest.cart.CartService;
import com.foodnest.common.BadRequestException;
import com.foodnest.dish.Dish;
import com.foodnest.dish.DishRepository;
import com.foodnest.restaurant.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartCalculationTest {

    @Mock
    private DishRepository dishRepository;

    @InjectMocks
    private CartService cartService;

    private Restaurant restaurantA;
    private Restaurant restaurantB;
    private Dish dish1;
    private Dish dish2;
    private Dish dishFromOtherRestaurant;

    @BeforeEach
    public void setup() {
        restaurantA = new Restaurant();
        restaurantA.setId(1L);
        restaurantA.setName("Fastfoods");

        restaurantB = new Restaurant();
        restaurantB.setId(2L);
        restaurantB.setName("Foodies");

        dish1 = new Dish();
        dish1.setId(101L);
        dish1.setName("Chicken Fry");
        dish1.setPrice(new BigDecimal("200.00"));
        dish1.setAvailableStock(10);
        dish1.setIsAvailable(true);
        dish1.setRestaurant(restaurantA);

        dish2 = new Dish();
        dish2.setId(102L);
        dish2.setName("Chicken 65");
        dish2.setPrice(new BigDecimal("100.00"));
        dish2.setAvailableStock(5);
        dish2.setIsAvailable(true);
        dish2.setRestaurant(restaurantA);

        dishFromOtherRestaurant = new Dish();
        dishFromOtherRestaurant.setId(201L);
        dishFromOtherRestaurant.setName("Mutton Fry");
        dishFromOtherRestaurant.setPrice(new BigDecimal("300.00"));
        dishFromOtherRestaurant.setAvailableStock(10);
        dishFromOtherRestaurant.setIsAvailable(true);
        dishFromOtherRestaurant.setRestaurant(restaurantB);
    }

    @Test
    public void testValidCartCalculation() {
        when(dishRepository.findById(101L)).thenReturn(Optional.of(dish1));
        when(dishRepository.findById(102L)).thenReturn(Optional.of(dish2));

        List<CartItemRequest> items = List.of(
                new CartItemRequest(101L, 2), // 200 * 2 = 400
                new CartItemRequest(102L, 1)  // 100 * 1 = 100
        );

        CartCalculationResponse response = cartService.calculateCart(items);

        assertNotNull(response);
        assertEquals(new BigDecimal("500.00"), response.getSubtotal());
        assertEquals(new BigDecimal("25.00"), response.getTaxAmount()); // 5% of 500 = 25.00
        assertEquals(new BigDecimal("40.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("565.00"), response.getTotalAmount()); // 500 + 25 + 40 = 565.00
        assertEquals(1L, response.getRestaurantId());
    }

    @Test
    public void testMultiRestaurantRejection() {
        when(dishRepository.findById(101L)).thenReturn(Optional.of(dish1));
        when(dishRepository.findById(201L)).thenReturn(Optional.of(dishFromOtherRestaurant));

        List<CartItemRequest> items = List.of(
                new CartItemRequest(101L, 1),
                new CartItemRequest(201L, 1)
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> cartService.calculateCart(items));
        assertTrue(ex.getMessage().contains("multiple restaurants"));
    }

    @Test
    public void testUnavailableDishRejection() {
        dish1.setIsAvailable(false);
        when(dishRepository.findById(101L)).thenReturn(Optional.of(dish1));

        List<CartItemRequest> items = List.of(new CartItemRequest(101L, 1));
        BadRequestException ex = assertThrows(BadRequestException.class, () -> cartService.calculateCart(items));
        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    public void testInsufficientStockRejection() {
        when(dishRepository.findById(102L)).thenReturn(Optional.of(dish2)); // stock is 5

        List<CartItemRequest> items = List.of(new CartItemRequest(102L, 10)); // requesting 10
        BadRequestException ex = assertThrows(BadRequestException.class, () -> cartService.calculateCart(items));
        assertTrue(ex.getMessage().contains("exceeds available stock"));
    }
}
