package com.foodnest.deliveryman;

import java.time.LocalDateTime;

public class DeliveryManDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String vehicleType;
    private Boolean isAvailable;
    private String accountStatus;
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastActiveAt;

    public DeliveryManDto() {
    }

    public static DeliveryManDto fromEntity(DeliveryMan dm) {
        DeliveryManDto dto = new DeliveryManDto();
        dto.setId(dm.getId());
        dto.setFullName(dm.getFullName());
        dto.setEmail(dm.getEmail());
        dto.setPhone(dm.getPhone());
        dto.setVehicleType(dm.getVehicleType());
        dto.setIsAvailable(dm.getIsAvailable());
        dto.setAccountStatus(dm.getAccountStatus());
        dto.setLatitude(dm.getLatitude());
        dto.setLongitude(dm.getLongitude());
        dto.setLastActiveAt(dm.getLastActiveAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
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

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
