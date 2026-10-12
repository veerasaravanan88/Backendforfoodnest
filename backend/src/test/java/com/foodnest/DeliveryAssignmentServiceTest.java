package com.foodnest;

import com.foodnest.delivery.AssignmentStatus;
import com.foodnest.delivery.DeliveryAssignment;
import com.foodnest.delivery.DeliveryAssignmentRepository;
import com.foodnest.delivery.DeliveryAssignmentService;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

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
}
