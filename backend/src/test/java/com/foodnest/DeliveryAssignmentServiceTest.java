package com.foodnest;

import com.foodnest.delivery.AssignmentStatus;
import com.foodnest.delivery.DeliveryAssignment;
import com.foodnest.delivery.DeliveryAssignmentRepository;
import com.foodnest.delivery.DeliveryAssignmentService;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.order.Order;
import com.foodnest.restaurant.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryAssignmentServiceTest {

    @Mock
    private DeliveryAssignmentRepository assignmentRepository;

    @Mock
    private DeliveryManRepository deliveryManRepository;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private DeliveryAssignmentService deliveryAssignmentService;

    private Order order;
    private DeliveryMan rider;

    @BeforeEach
    public void setup() {
        order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-20261012-ABC123");

        rider = new DeliveryMan();
        rider.setId(5L);
        rider.setFullName("David Vance");
        rider.setIsAvailable(true);
        rider.setAccountStatus("ACTIVE");
    }

    @Test
    public void testSuccessfulAssignmentWhenRiderAvailable() {
        when(assignmentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(deliveryManRepository.findAvailableRidersForAssignmentLocked("ACTIVE"))
                .thenReturn(List.of(rider));
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryAssignment assignment = deliveryAssignmentService.assignDeliveryMan(order);

        assertNotNull(assignment);
        assertEquals(AssignmentStatus.ASSIGNED, assignment.getStatus());
        assertEquals(rider, assignment.getDeliveryMan());
        assertFalse(rider.getIsAvailable()); // Rider should be marked busy

        verify(deliveryManRepository).save(rider);
        verify(assignmentRepository).save(any(DeliveryAssignment.class));
    }

    @Test
    public void testQueuedWhenNoRiderAvailable() {
        when(assignmentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(deliveryManRepository.findAvailableRidersForAssignmentLocked("ACTIVE"))
                .thenReturn(Collections.emptyList());
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryAssignment assignment = deliveryAssignmentService.assignDeliveryMan(order);

        assertNotNull(assignment);
        assertEquals(AssignmentStatus.AWAITING_ASSIGNMENT, assignment.getStatus());
        assertNull(assignment.getDeliveryMan());
    }

    @Test
    public void testNearestRiderPrioritizedOverFartherRider() {
        Restaurant restaurant = new Restaurant();
        restaurant.setLatitude(12.9756);
        restaurant.setLongitude(77.6066);
        order.setRestaurant(restaurant);

        // Far rider (5 km away)
        DeliveryMan farRider = new DeliveryMan();
        farRider.setId(1L);
        farRider.setFullName("Far Rider");
        farRider.setLatitude(13.0500);
        farRider.setLongitude(77.6500);
        farRider.setIsAvailable(true);
        farRider.setAccountStatus("ACTIVE");

        // Close rider (0.1 km away)
        DeliveryMan closeRider = new DeliveryMan();
        closeRider.setId(5L);
        closeRider.setFullName("Nagu (Close Rider)");
        closeRider.setLatitude(12.9758);
        closeRider.setLongitude(77.6068);
        closeRider.setIsAvailable(true);
        closeRider.setAccountStatus("ACTIVE");

        when(assignmentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(deliveryManRepository.findAvailableRidersForAssignmentLocked("ACTIVE"))
                .thenReturn(List.of(farRider, closeRider));
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryAssignment assignment = deliveryAssignmentService.assignDeliveryMan(order);

        assertNotNull(assignment);
        assertEquals(closeRider, assignment.getDeliveryMan()); // Nagu is chosen because he is closest!
        assertFalse(closeRider.getIsAvailable());
    }

    @Test
    public void testMostRecentlyActiveRiderPrioritizedOverStaleRider() {
        // Stale rider (e.g. idle/inactive for hours)
        DeliveryMan staleRider = new DeliveryMan();
        staleRider.setId(1L);
        staleRider.setFullName("David Vance");
        staleRider.setLastActiveAt(LocalDateTime.now().minusHours(3));
        staleRider.setIsAvailable(true);
        staleRider.setAccountStatus("ACTIVE");

        // Active rider (e.g. Nagu active on dashboard 10 seconds ago)
        DeliveryMan activeRider = new DeliveryMan();
        activeRider.setId(5L);
        activeRider.setFullName("Nagu");
        activeRider.setLastActiveAt(LocalDateTime.now().minusSeconds(10));
        activeRider.setIsAvailable(true);
        activeRider.setAccountStatus("ACTIVE");

        when(assignmentRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(deliveryManRepository.findAvailableRidersForAssignmentLocked("ACTIVE"))
                .thenReturn(List.of(staleRider, activeRider));
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeliveryAssignment assignment = deliveryAssignmentService.assignDeliveryMan(order);

        assertNotNull(assignment);
        assertEquals(activeRider, assignment.getDeliveryMan()); // Nagu chosen over stale rider!
    }

    @Test
    public void testClaimOrderSuccess() {
        DeliveryAssignment pendingAssignment = new DeliveryAssignment();
        pendingAssignment.setId(1L);
        pendingAssignment.setOrder(order);
        pendingAssignment.setStatus(AssignmentStatus.AWAITING_ASSIGNMENT);

        when(deliveryManRepository.findById(5L)).thenReturn(Optional.of(rider));
        when(assignmentRepository.findByOrderId(10L)).thenReturn(Optional.of(pendingAssignment));
        when(assignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryAssignment claimed = deliveryAssignmentService.claimOrder(10L, 5L);

        assertNotNull(claimed);
        assertEquals(AssignmentStatus.ASSIGNED, claimed.getStatus());
        assertEquals(rider, claimed.getDeliveryMan());
        assertFalse(rider.getIsAvailable());
    }
}
