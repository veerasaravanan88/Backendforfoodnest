package com.foodnest.delivery;

import com.foodnest.common.BadRequestException;
import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.order.Order;
import com.foodnest.restaurant.Restaurant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeliveryAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAssignmentService.class);

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryManRepository deliveryManRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    public DeliveryAssignmentService(DeliveryAssignmentRepository assignmentRepository,
                                     DeliveryManRepository deliveryManRepository,
                                     SimpMessageSendingOperations messagingTemplate) {
        this.assignmentRepository = assignmentRepository;
        this.deliveryManRepository = deliveryManRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public DeliveryAssignment assignDeliveryMan(Order order) {
        log.info("Attempting automatic delivery assignment for Order ID: {} ({})", order.getId(), order.getOrderNumber());

        DeliveryAssignment assignment = assignmentRepository.findByOrderId(order.getId())
                .orElseGet(() -> {
                    DeliveryAssignment da = new DeliveryAssignment();
                    da.setOrder(order);
                    da.setStatus(AssignmentStatus.AWAITING_ASSIGNMENT);
                    return da;
                });

        // If already assigned to an active rider, keep existing assignment
        if (assignment.getDeliveryMan() != null && assignment.getStatus() != AssignmentStatus.AWAITING_ASSIGNMENT) {
            log.info("Order ID: {} is already assigned to rider ID: {}", order.getId(), assignment.getDeliveryMan().getId());
            return assignment;
        }

        // Concurrency-safe selection using pessimistic lock for riders with isAvailable = true
        List<DeliveryMan> availableRiders = deliveryManRepository.findAvailableRidersForAssignmentLocked("ACTIVE");

        if (availableRiders.isEmpty()) {
            log.warn("No available delivery partner found for Order ID: {}. Retaining in unassigned queue (AWAITING_ASSIGNMENT).", order.getId());
            assignment.setStatus(AssignmentStatus.AWAITING_ASSIGNMENT);
            DeliveryAssignment saved = assignmentRepository.save(assignment);
            broadcastAvailableOrders();
            return saved;
        }

        // Determine pickup restaurant location
        double[] coords = resolveRestaurantCoordinates(order.getRestaurant());
        final double restLat = coords[0];
        final double restLon = coords[1];

        // Intelligent ranking by proximity and recent online activity:
        // 1. Closest rider with live GPS broadcast
        // 2. Most recently active online rider (heartbeat / availability toggle)
        // 3. Higher ID (recently active / registered partners over legacy seed ID 1)
        List<DeliveryMan> sortedRiders = new ArrayList<>(availableRiders);
        sortedRiders.sort((r1, r2) -> {
            boolean r1HasCoords = r1.getLatitude() != null && r1.getLongitude() != null;
            boolean r2HasCoords = r2.getLatitude() != null && r2.getLongitude() != null;

            if (r1HasCoords && r2HasCoords) {
                double d1 = calculateDistanceKm(restLat, restLon, r1.getLatitude(), r1.getLongitude());
                double d2 = calculateDistanceKm(restLat, restLon, r2.getLatitude(), r2.getLongitude());
                int cmp = Double.compare(d1, d2);
                if (cmp != 0) return cmp;
            } else if (r1HasCoords) {
                return -1; // Preferred: has live GPS coordinates
            } else if (r2HasCoords) {
                return 1;
            }

            LocalDateTime t1 = r1.getLastActiveAt() != null ? r1.getLastActiveAt() : r1.getUpdatedAt();
            LocalDateTime t2 = r2.getLastActiveAt() != null ? r2.getLastActiveAt() : r2.getUpdatedAt();
            if (t1 != null && t2 != null) {
                int timeCmp = t2.compareTo(t1); // Most recently active first
                if (timeCmp != 0) return timeCmp;
            } else if (t1 != null) {
                return -1;
            } else if (t2 != null) {
                return 1;
            }

            return Long.compare(r2.getId(), r1.getId());
        });

        DeliveryMan selectedRider = sortedRiders.get(0);
        selectedRider.setIsAvailable(false);
        selectedRider.setLastActiveAt(LocalDateTime.now());
        deliveryManRepository.save(selectedRider);

        assignment.setDeliveryMan(selectedRider);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());

        DeliveryAssignment saved = assignmentRepository.save(assignment);
        log.info("Successfully assigned online rider ID: {} ({}) to Order ID: {}", selectedRider.getId(), selectedRider.getFullName(), order.getId());

        // Notify client subscribers via WebSocket
        try {
            DeliveryAssignmentDto dto = DeliveryAssignmentDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), dto);
            messagingTemplate.convertAndSend("/topic/riders/" + selectedRider.getId() + "/assignments", dto);
            messagingTemplate.convertAndSend("/topic/riders/broadcast", dto);
        } catch (Exception e) {
            log.warn("Could not dispatch WebSocket notification for assignment: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public DeliveryAssignment claimOrder(Long orderId, Long deliveryManId) {
        DeliveryMan rider = deliveryManRepository.findById(deliveryManId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + deliveryManId));

        if (!Boolean.TRUE.equals(rider.getIsAvailable()) || !"ACTIVE".equals(rider.getAccountStatus())) {
            throw new BadRequestException("You must be Online (Available) to accept deliveries.");
        }

        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found for order id: " + orderId));

        if (assignment.getStatus() != AssignmentStatus.AWAITING_ASSIGNMENT) {
            throw new BadRequestException("This order is already assigned to a delivery partner.");
        }

        rider.setIsAvailable(false);
        rider.setLastActiveAt(LocalDateTime.now());
        deliveryManRepository.save(rider);

        assignment.setDeliveryMan(rider);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        DeliveryAssignment saved = assignmentRepository.save(assignment);

        try {
            DeliveryAssignmentDto dto = DeliveryAssignmentDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/orders/" + orderId, dto);
            messagingTemplate.convertAndSend("/topic/riders/" + rider.getId() + "/assignments", dto);
            messagingTemplate.convertAndSend("/topic/riders/broadcast", dto);
        } catch (Exception ignored) {
        }

        return saved;
    }

    @Transactional
    public void retryAwaitingAssignments() {
        List<DeliveryAssignment> awaiting = assignmentRepository.findByStatus(AssignmentStatus.AWAITING_ASSIGNMENT);
        for (DeliveryAssignment da : awaiting) {
            assignDeliveryMan(da.getOrder());
        }
    }

    private void broadcastAvailableOrders() {
        try {
            messagingTemplate.convertAndSend("/topic/riders/broadcast", Collections.singletonMap("type", "UNASSIGNED_ORDERS_UPDATED"));
        } catch (Exception ignored) {
        }
    }

    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double[] resolveRestaurantCoordinates(Restaurant restaurant) {
        if (restaurant != null) {
            if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
                return new double[]{restaurant.getLatitude(), restaurant.getLongitude()};
            }
            if (restaurant.getAddress() != null) {
                String addr = restaurant.getAddress().toLowerCase();
                if (addr.contains("bengaluru") || addr.contains("bangalore") || addr.contains("560")) {
                    return new double[]{12.9716, 77.5946};
                }
                if (addr.contains("chennai") || addr.contains("600")) {
                    return new double[]{13.0827, 80.2707};
                }
                if (addr.contains("coimbatore") || addr.contains("641")) {
                    return new double[]{11.0168, 76.9558};
                }
                if (addr.contains("trichy") || addr.contains("620") || addr.contains("621")) {
                    return new double[]{10.7905, 78.7047};
                }
                if (addr.contains("kallupatti") || addr.contains("625702") || addr.contains("625703")) {
                    return new double[]{9.7206, 77.8517};
                }
                if (addr.contains("madurai") || addr.contains("maduari") || addr.contains("625")) {
                    return new double[]{9.9252, 78.1198};
                }
            }
        }
        return new double[]{9.7206, 77.8517}; // Regional default: Madurai / T.Kallupatti
    }
}
