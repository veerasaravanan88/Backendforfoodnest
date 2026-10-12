package com.foodnest.tracking;

import jakarta.validation.constraints.NotNull;

public class LocationUpdateRequest {

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double heading;

    public LocationUpdateRequest() {
    }

    public LocationUpdateRequest(Double latitude, Double longitude, Double heading) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }
}
