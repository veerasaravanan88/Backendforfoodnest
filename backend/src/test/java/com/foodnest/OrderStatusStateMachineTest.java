package com.foodnest;

import com.foodnest.order.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderStatusStateMachineTest {

    @Test
    public void testValidTransitions() {
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.ACCEPTED));
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED));

        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.CANCELLED));

        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.READY));

        assertTrue(OrderStatus.READY.canTransitionTo(OrderStatus.PICKED_UP));

        assertTrue(OrderStatus.PICKED_UP.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY));

        assertTrue(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    public void testInvalidTransitions() {
        // Cannot jump from PLACED directly to DELIVERED
        assertFalse(OrderStatus.PLACED.canTransitionTo(OrderStatus.DELIVERED));

        // Cannot jump from PLACED to READY
        assertFalse(OrderStatus.PLACED.canTransitionTo(OrderStatus.READY));

        // Cannot jump from PREPARING to DELIVERED
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.DELIVERED));

        // Once DELIVERED, cannot transition anywhere
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PLACED));

        // Once CANCELLED, cannot transition anywhere
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.ACCEPTED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DELIVERED));
    }
}
