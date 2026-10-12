package com.foodnest.deliveryman;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryManRepository extends JpaRepository<DeliveryMan, Long> {
    Optional<DeliveryMan> findByEmail(String email);
    Optional<DeliveryMan> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    List<DeliveryMan> findByIsAvailableTrueAndAccountStatus(String accountStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DeliveryMan d WHERE d.isAvailable = true AND d.accountStatus = :accountStatus ORDER BY d.id ASC")
    List<DeliveryMan> findAvailableRidersForAssignmentLocked(String accountStatus);
}
