package com.foodnest.deliveryman;

import jakarta.validation.constraints.NotNull;

public class AvailabilityUpdateRequest {

    @NotNull(message = "isAvailable must be provided")
    private Boolean isAvailable;

    public AvailabilityUpdateRequest() {
    }

    public AvailabilityUpdateRequest(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
