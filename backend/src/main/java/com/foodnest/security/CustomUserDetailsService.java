package com.foodnest.security;

import com.foodnest.customer.Customer;
import com.foodnest.customer.CustomerRepository;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.restaurant.Restaurant;
import com.foodnest.restaurant.RestaurantRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryManRepository deliveryManRepository;

    public CustomUserDetailsService(CustomerRepository customerRepository,
                                    RestaurantRepository restaurantRepository,
                                    DeliveryManRepository deliveryManRepository) {
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryManRepository = deliveryManRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Try Customer
        Optional<Customer> customerOpt = customerRepository.findByEmail(username);
        if (customerOpt.isPresent()) {
            Customer c = customerOpt.get();
            return new UserPrincipal(c.getId(), c.getEmail(), c.getPasswordHash(), c.getFullName(), UserRole.CUSTOMER);
        }

        // 2. Try Restaurant
        Optional<Restaurant> restaurantOpt = restaurantRepository.findByEmail(username);
        if (restaurantOpt.isPresent()) {
            Restaurant r = restaurantOpt.get();
            return new UserPrincipal(r.getId(), r.getEmail(), r.getPasswordHash(), r.getName(), UserRole.RESTAURANT);
        }

        // 3. Try Delivery Man
        Optional<DeliveryMan> deliveryManOpt = deliveryManRepository.findByEmail(username);
        if (deliveryManOpt.isPresent()) {
            DeliveryMan d = deliveryManOpt.get();
            return new UserPrincipal(d.getId(), d.getEmail(), d.getPasswordHash(), d.getFullName(), UserRole.DELIVERY_MAN);
        }

        throw new UsernameNotFoundException("User not found with identifier: " + username);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadByRoleAndEmail(UserRole role, String email) {
        return switch (role) {
            case CUSTOMER -> customerRepository.findByEmail(email)
                    .map(c -> new UserPrincipal(c.getId(), c.getEmail(), c.getPasswordHash(), c.getFullName(), UserRole.CUSTOMER))
                    .orElseThrow(() -> new UsernameNotFoundException("Customer account not found with email: " + email));
            case RESTAURANT -> restaurantRepository.findByEmail(email)
                    .map(r -> new UserPrincipal(r.getId(), r.getEmail(), r.getPasswordHash(), r.getName(), UserRole.RESTAURANT))
                    .orElseThrow(() -> new UsernameNotFoundException("Restaurant account not found with email: " + email));
            case DELIVERY_MAN -> deliveryManRepository.findByEmail(email)
                    .map(d -> new UserPrincipal(d.getId(), d.getEmail(), d.getPasswordHash(), d.getFullName(), UserRole.DELIVERY_MAN))
                    .orElseThrow(() -> new UsernameNotFoundException("Delivery partner account not found with email: " + email));
        };
    }
}
