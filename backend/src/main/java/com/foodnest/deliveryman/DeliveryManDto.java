package com.foodnest.deliveryman;

public class DeliveryManDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String vehicleType;
    private Boolean isAvailable;
    private String accountStatus;

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
}
