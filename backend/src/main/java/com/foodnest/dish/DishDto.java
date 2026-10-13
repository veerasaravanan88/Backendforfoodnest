package com.foodnest.dish;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DishDto {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer availableStock;
    private String imageUrl;
    private Boolean isAvailable;
    private LocalDateTime createdAt;

    public DishDto() {
    }

    public static DishDto fromEntity(Dish dish) {
        DishDto dto = new DishDto();
        dto.setId(dish.getId());
        if (dish.getRestaurant() != null) {
            dto.setRestaurantId(dish.getRestaurant().getId());
            dto.setRestaurantName(dish.getRestaurant().getName());
        }
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setCategory(dish.getCategory());
        dto.setPrice(dish.getPrice());
        dto.setAvailableStock(dish.getAvailableStock());
        dto.setImageUrl(dish.getImageUrl());
        dto.setIsAvailable(dish.getIsAvailable());
        dto.setCreatedAt(dish.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
