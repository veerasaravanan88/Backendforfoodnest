package com.foodnest.security;

public enum UserRole {
    CUSTOMER,
    RESTAURANT,
    DELIVERY_MAN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
