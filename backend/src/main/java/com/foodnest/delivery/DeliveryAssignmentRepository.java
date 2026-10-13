package com.foodnest.delivery;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    @EntityGraph(attributePaths = {"order", "deliveryMan"})
    Optional<DeliveryAssignment> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "deliveryMan"})
    List<DeliveryAssignment> findByDeliveryManIdOrderByAssignedAtDesc(Long deliveryManId);

    @EntityGraph(attributePaths = {"order", "deliveryMan"})
    Optional<DeliveryAssignment> findByIdAndDeliveryManId(Long id, Long deliveryManId);

    @EntityGraph(attributePaths = {"order", "deliveryMan"})
    Optional<DeliveryAssignment> findFirstByDeliveryManIdAndStatusInOrderByAssignedAtDesc(
            Long deliveryManId, List<AssignmentStatus> statuses);

    List<DeliveryAssignment> findByStatus(AssignmentStatus status);

    boolean existsByOrderId(Long orderId);
}
