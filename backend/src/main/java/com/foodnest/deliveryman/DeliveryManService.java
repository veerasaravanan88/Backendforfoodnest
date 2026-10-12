package com.foodnest.deliveryman;

import com.foodnest.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryManService {

    private final DeliveryManRepository deliveryManRepository;

    public DeliveryManService(DeliveryManRepository deliveryManRepository) {
        this.deliveryManRepository = deliveryManRepository;
    }

    @Transactional(readOnly = true)
    public DeliveryManDto getProfile(Long id) {
        DeliveryMan dm = deliveryManRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + id));
        return DeliveryManDto.fromEntity(dm);
    }

    @Transactional
    public DeliveryManDto updateAvailability(Long id, boolean isAvailable) {
        DeliveryMan dm = deliveryManRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found with id: " + id));
        dm.setIsAvailable(isAvailable);
        DeliveryMan saved = deliveryManRepository.save(dm);
        return DeliveryManDto.fromEntity(saved);
    }
}
