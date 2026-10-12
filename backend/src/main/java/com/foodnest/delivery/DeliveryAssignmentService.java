package com.foodnest.delivery;

import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

        // Concurrency-safe selection using pessimistic lock
        List<DeliveryMan> availableRiders = deliveryManRepository.findAvailableRidersForAssignmentLocked("ACTIVE");

        if (availableRiders.isEmpty()) {
            log.warn("No available delivery partner found for Order ID: {}. Retaining in unassigned queue (AWAITING_ASSIGNMENT).", order.getId());
            assignment.setStatus(AssignmentStatus.AWAITING_ASSIGNMENT);
            return assignmentRepository.save(assignment);
        }

        // Fair least-active / first available policy
        DeliveryMan selectedRider = availableRiders.get(0);
        selectedRider.setIsAvailable(false);
        deliveryManRepository.save(selectedRider);

        assignment.setDeliveryMan(selectedRider);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());

        DeliveryAssignment saved = assignmentRepository.save(assignment);
        log.info("Successfully assigned rider ID: {} ({}) to Order ID: {}", selectedRider.getId(), selectedRider.getFullName(), order.getId());

        // Notify client subscribers via WebSocket
        try {
            DeliveryAssignmentDto dto = DeliveryAssignmentDto.fromEntity(saved);
            messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), dto);
            messagingTemplate.convertAndSend("/topic/riders/" + selectedRider.getId() + "/assignments", dto);
        } catch (Exception e) {
            log.warn("Could not dispatch WebSocket notification for assignment: {}", e.getMessage());
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
}
