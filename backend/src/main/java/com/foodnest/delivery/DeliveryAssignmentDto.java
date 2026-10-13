package com.foodnest.delivery;

import java.time.LocalDateTime;

public class DeliveryAssignmentDto {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long deliveryManId;
    private String deliveryManName;
    private String deliveryManPhone;
    private String vehicleType;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    public DeliveryAssignmentDto() {
    }

    public static DeliveryAssignmentDto fromEntity(DeliveryAssignment assignment) {
        DeliveryAssignmentDto dto = new DeliveryAssignmentDto();
        dto.setId(assignment.getId());
        if (assignment.getOrder() != null) {
            dto.setOrderId(assignment.getOrder().getId());
            dto.setOrderNumber(assignment.getOrder().getOrderNumber());
        }
        if (assignment.getDeliveryMan() != null) {
            dto.setDeliveryManId(assignment.getDeliveryMan().getId());
            dto.setDeliveryManName(assignment.getDeliveryMan().getFullName());
            dto.setDeliveryManPhone(assignment.getDeliveryMan().getPhone());
            dto.setVehicleType(assignment.getDeliveryMan().getVehicleType());
        }
        dto.setStatus(assignment.getStatus());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setPickedUpAt(assignment.getPickedUpAt());
        dto.setDeliveredAt(assignment.getDeliveredAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getDeliveryManId() {
        return deliveryManId;
    }

    public void setDeliveryManId(Long deliveryManId) {
        this.deliveryManId = deliveryManId;
    }

    public String getDeliveryManName() {
        return deliveryManName;
    }

    public void setDeliveryManName(String deliveryManName) {
        this.deliveryManName = deliveryManName;
    }

    public String getDeliveryManPhone() {
        return deliveryManPhone;
    }

    public void setDeliveryManPhone(String deliveryManPhone) {
        this.deliveryManPhone = deliveryManPhone;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
