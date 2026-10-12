package com.foodnest.tracking;

import com.foodnest.common.BadRequestException;
import com.foodnest.common.ForbiddenException;
import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.delivery.AssignmentStatus;
import com.foodnest.delivery.DeliveryAssignment;
import com.foodnest.delivery.DeliveryAssignmentRepository;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.order.Order;
import com.foodnest.order.OrderStatus;
import com.foodnest.security.UserPrincipal;
import com.foodnest.security.UserRole;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Service
public class TrackingService {

    private final DeliveryLocationRepository locationRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryManRepository deliveryManRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TrackingService(DeliveryLocationRepository locationRepository,
                           DeliveryAssignmentRepository assignmentRepository,
                           DeliveryManRepository deliveryManRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.locationRepository = locationRepository;
        this.assignmentRepository = assignmentRepository;
        this.deliveryManRepository = deliveryManRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public LocationResponse saveLocation(Long deliveryManId, LocationUpdateRequest req) {
        DeliveryMan rider = deliveryManRepository.findById(deliveryManId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + deliveryManId));

        // Look for active assignments: ASSIGNED, ACCEPTED, PICKED_UP
        DeliveryAssignment assignment = assignmentRepository
                .findFirstByDeliveryManIdAndStatusInOrderByAssignedAtDesc(
                        deliveryManId,
                        Arrays.asList(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP)
                )
                .orElseThrow(() -> new BadRequestException("No active delivery assignment currently in progress to track."));

        Order order = assignment.getOrder();
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already finished. Location tracking has terminated.");
        }

        DeliveryLocation loc = new DeliveryLocation();
        loc.setDeliveryAssignment(assignment);
        loc.setDeliveryMan(rider);
        loc.setLatitude(req.getLatitude());
        loc.setLongitude(req.getLongitude());
        loc.setHeading(req.getHeading());

        DeliveryLocation saved = locationRepository.save(loc);

        LocationResponse response = buildLocationResponse(order, assignment, rider, saved);

        // Push real-time update via STOMP WebSocket
        try {
            messagingTemplate.convertAndSend("/topic/tracking/orders/" + order.getId(), response);
        } catch (Exception ignored) {
        }

        return response;
    }

    @Transactional(readOnly = true)
    public LocationResponse getTrackingForOrder(Long orderId, UserPrincipal principal) {
        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No delivery tracking found for order id: " + orderId));

        Order order = assignment.getOrder();

        // Privacy check: only order customer, assigned rider, or order restaurant can access
        boolean authorized = false;
        if (principal.getRole() == UserRole.CUSTOMER && order.getCustomer().getId().equals(principal.getId())) {
            authorized = true;
        } else if (principal.getRole() == UserRole.DELIVERY_MAN && assignment.getDeliveryMan() != null
                && assignment.getDeliveryMan().getId().equals(principal.getId())) {
            authorized = true;
        } else if (principal.getRole() == UserRole.RESTAURANT && order.getRestaurant().getId().equals(principal.getId())) {
            authorized = true;
        }

        if (!authorized) {
            throw new ForbiddenException("Access denied: You are not authorized to view tracking for this order.");
        }

        DeliveryMan rider = assignment.getDeliveryMan();
        Optional<DeliveryLocation> latestLoc = locationRepository
                .findFirstByDeliveryAssignmentIdOrderByUpdatedAtDesc(assignment.getId());

        return buildLocationResponse(order, assignment, rider, latestLoc.orElse(null));
    }

    private LocationResponse buildLocationResponse(Order order, DeliveryAssignment assignment, DeliveryMan rider, DeliveryLocation loc) {
        LocationResponse res = new LocationResponse();
        res.setOrderId(order.getId());
        res.setOrderNumber(order.getOrderNumber());
        res.setOrderStatus(order.getStatus());

        if (rider != null) {
            res.setDeliveryManId(rider.getId());
            res.setDeliveryManName(rider.getFullName());
            res.setDeliveryManPhone(rider.getPhone());
        }

        if (loc != null) {
            res.setLatitude(loc.getLatitude());
            res.setLongitude(loc.getLongitude());
            res.setHeading(loc.getHeading());
            res.setUpdatedAt(loc.getUpdatedAt());
        }

        boolean isActive = order.getStatus() != OrderStatus.DELIVERED &&
                order.getStatus() != OrderStatus.CANCELLED &&
                rider != null;
        res.setIsLiveTrackingActive(isActive);

        return res;
    }
}
