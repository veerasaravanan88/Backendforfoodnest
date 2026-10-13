package com.foodnest.report;

import com.foodnest.dish.Dish;
import com.foodnest.dish.DishRepository;
import com.foodnest.order.Order;
import com.foodnest.order.OrderDto;
import com.foodnest.order.OrderRepository;
import com.foodnest.order.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;

    public ReportService(OrderRepository orderRepository, DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
    }

    @Transactional(readOnly = true)
    public RestaurantDashboardReportDto getDashboardReport(Long restaurantId) {
        RestaurantDashboardReportDto report = new RestaurantDashboardReportDto();

        List<Order> allOrders = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        List<Dish> allDishes = dishRepository.findByRestaurantId(restaurantId);

        long totalOrders = allOrders.size();
        long activeCount = 0;
        long deliveredCount = 0;
        long cancelledCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Order o : allOrders) {
            if (o.getStatus() == OrderStatus.DELIVERED) {
                deliveredCount++;
                totalRevenue = totalRevenue.add(o.getTotalAmount());
            } else if (o.getStatus() == OrderStatus.CANCELLED) {
                cancelledCount++;
            } else {
                activeCount++;
            }
        }

        long totalDishes = allDishes.size();
        long availableDishes = allDishes.stream().filter(d -> Boolean.TRUE.equals(d.getIsAvailable())).count();

        report.setTotalOrders(totalOrders);
        report.setTotalRevenue(totalRevenue);
        report.setActiveOrders(activeCount);
        report.setDeliveredOrders(deliveredCount);
        report.setCancelledOrders(cancelledCount);
        report.setTotalDishes(totalDishes);
        report.setAvailableDishes(availableDishes);

        // Recent 5 orders
        List<OrderDto> recent = allOrders.stream()
                .limit(5)
                .map(OrderDto::fromEntity)
                .collect(Collectors.toList());
        report.setRecentOrders(recent);

        // Daily sales breakdown for last 7 days
        Map<LocalDate, BigDecimal> dailyRevMap = new LinkedHashMap<>();
        Map<LocalDate, Long> dailyOrderMap = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailyRevMap.put(d, BigDecimal.ZERO);
            dailyOrderMap.put(d, 0L);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd");
        for (Order o : allOrders) {
            if (o.getCreatedAt() != null) {
                LocalDate od = o.getCreatedAt().toLocalDate();
                if (dailyRevMap.containsKey(od)) {
                    if (o.getStatus() != OrderStatus.CANCELLED) {
                        dailyRevMap.put(od, dailyRevMap.get(od).add(o.getTotalAmount()));
                        dailyOrderMap.put(od, dailyOrderMap.get(od) + 1);
                    }
                }
            }
        }

        List<RestaurantDashboardReportDto.DailySalesDto> salesList = new ArrayList<>();
        dailyRevMap.forEach((date, rev) -> {
            salesList.add(new RestaurantDashboardReportDto.DailySalesDto(
                    date.format(dtf),
                    rev,
                    dailyOrderMap.get(date)
            ));
        });
        report.setDailySales(salesList);

        return report;
    }
}
