package com.foodnest.cart;

import java.math.BigDecimal;

public class CartCalculationItemResponse {
    private Long dishId;
    private String name;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal itemTotal;
    private String imageUrl;
    private Boolean isAvailable;

    public CartCalculationItemResponse() {
    }

    public CartCalculationItemResponse(Long dishId, String name, BigDecimal unitPrice, Integer quantity, BigDecimal itemTotal, String imageUrl, Boolean isAvailable) {
        this.dishId = dishId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.itemTotal = itemTotal;
        this.imageUrl = imageUrl;
        this.isAvailable = isAvailable;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(BigDecimal itemTotal) {
        this.itemTotal = itemTotal;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
