package com.foodnest.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Restaurant> findByAccountStatus(String accountStatus);
    List<Restaurant> findByAccountStatusAndCuisineTypeContainingIgnoreCase(String accountStatus, String cuisine);
    List<Restaurant> findByAccountStatusAndNameContainingIgnoreCase(String accountStatus, String name);
}
