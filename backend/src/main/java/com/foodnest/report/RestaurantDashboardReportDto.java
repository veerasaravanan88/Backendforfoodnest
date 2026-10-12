package com.foodnest.report;

import com.foodnest.order.OrderDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDashboardReportDto {

    private long totalOrders;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long activeOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long totalDishes;
    private long availableDishes;
    private List<OrderDto> recentOrders = new ArrayList<>();
    private List<DailySalesDto> dailySales = new ArrayList<>();

    public RestaurantDashboardReportDto() {
    }

    public static class DailySalesDto {
        private String date;
        private BigDecimal revenue;
        private long orders;

        public DailySalesDto() {
        }

        public DailySalesDto(String date, BigDecimal revenue, long orders) {
            this.date = date;
            this.revenue = revenue;
            this.orders = orders;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        public long getOrders() {
            return orders;
        }

        public void setOrders(long orders) {
            this.orders = orders;
        }
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(long activeOrders) {
        this.activeOrders = activeOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public long getTotalDishes() {
        return totalDishes;
    }

    public void setTotalDishes(long totalDishes) {
        this.totalDishes = totalDishes;
    }

    public long getAvailableDishes() {
        return availableDishes;
    }

    public void setAvailableDishes(long availableDishes) {
        this.availableDishes = availableDishes;
    }

    public List<OrderDto> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderDto> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<DailySalesDto> getDailySales() {
        return dailySales;
    }

    public void setDailySales(List<DailySalesDto> dailySales) {
        this.dailySales = dailySales;
    }
}
