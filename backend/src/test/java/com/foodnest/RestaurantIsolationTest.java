package com.foodnest;

import com.foodnest.common.ForbiddenException;
import com.foodnest.dish.Dish;
import com.foodnest.dish.DishRepository;
import com.foodnest.dish.DishService;
import com.foodnest.dish.UpdateDishRequest;
import com.foodnest.restaurant.Restaurant;
import com.foodnest.restaurant.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestaurantIsolationTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private DishService dishService;

    private Restaurant fastfoods;
    private Restaurant foodies;
    private Dish chickenFry;
    private Dish muttonFry;

    @BeforeEach
    public void setup() {
        fastfoods = new Restaurant();
        fastfoods.setId(1L);
        fastfoods.setName("Fastfoods");

        foodies = new Restaurant();
        foodies.setId(2L);
        foodies.setName("Foodies");

        chickenFry = new Dish();
        chickenFry.setId(101L);
        chickenFry.setName("Chicken Fry");
        chickenFry.setRestaurant(fastfoods);
        chickenFry.setPrice(new BigDecimal("199.00"));

        muttonFry = new Dish();
        muttonFry.setId(201L);
        muttonFry.setName("Mutton Fry");
        muttonFry.setRestaurant(foodies);
        muttonFry.setPrice(new BigDecimal("299.00"));
    }

    @Test
    public void testCannotModifyAnotherRestaurantsDish() {
        // Fastfoods (ID 1) tries to edit Foodies' dish (ID 201)
        when(dishRepository.findById(201L)).thenReturn(Optional.of(muttonFry));

        UpdateDishRequest req = new UpdateDishRequest();
        req.setName("Hacked Dish");
        req.setCategory("Starters");
        req.setPrice(new BigDecimal("50.00"));
        req.setAvailableStock(10);
        req.setIsAvailable(true);

        assertThrows(ForbiddenException.class, () -> {
            dishService.updateDish(1L, 201L, req);
        });
    }

    @Test
    public void testCannotDeleteAnotherRestaurantsDish() {
        // Fastfoods (ID 1) tries to delete Foodies' dish (ID 201)
        when(dishRepository.findById(201L)).thenReturn(Optional.of(muttonFry));

        assertThrows(ForbiddenException.class, () -> {
            dishService.deleteDish(1L, 201L);
        });
    }
}
