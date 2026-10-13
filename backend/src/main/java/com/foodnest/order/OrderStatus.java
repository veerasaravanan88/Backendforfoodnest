package com.foodnest.order;

public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        if (this == next) return true;
        return switch (this) {
            case PLACED -> (next == ACCEPTED || next == CANCELLED);
            case ACCEPTED -> (next == PREPARING || next == CANCELLED);
            case PREPARING -> (next == READY);
            case READY -> (next == PICKED_UP);
            case PICKED_UP -> (next == OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> (next == DELIVERED);
            case DELIVERED, CANCELLED -> false;
        };
    }
}
