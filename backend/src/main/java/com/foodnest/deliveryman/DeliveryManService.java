package com.foodnest.deliveryman;

import com.foodnest.common.ResourceNotFoundException;
import com.foodnest.delivery.DeliveryAssignmentService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DeliveryManService {

    private final DeliveryManRepository deliveryManRepository;
    private final ObjectProvider<DeliveryAssignmentService> deliveryAssignmentServiceProvider;

    public DeliveryManService(DeliveryManRepository deliveryManRepository,
                              ObjectProvider<DeliveryAssignmentService> deliveryAssignmentServiceProvider) {
        this.deliveryManRepository = deliveryManRepository;
        this.deliveryAssignmentServiceProvider = deliveryAssignmentServiceProvider;
    }

    @Transactional
    public DeliveryManDto getProfile(Long id) {
        DeliveryMan dm = deliveryManRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + id));
        dm.setLastActiveAt(LocalDateTime.now());
        DeliveryMan saved = deliveryManRepository.save(dm);
        return DeliveryManDto.fromEntity(saved);
    }

    @Transactional
    public DeliveryManDto updateAvailability(Long id, boolean isAvailable) {
        DeliveryMan dm = deliveryManRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + id));
        dm.setIsAvailable(isAvailable);
        dm.setLastActiveAt(LocalDateTime.now());
        DeliveryMan saved = deliveryManRepository.save(dm);

        if (isAvailable) {
            DeliveryAssignmentService assignmentService = deliveryAssignmentServiceProvider.getIfAvailable();
            if (assignmentService != null) {
                assignmentService.retryAwaitingAssignments();
            }
        }

        return DeliveryManDto.fromEntity(saved);
    }
}
