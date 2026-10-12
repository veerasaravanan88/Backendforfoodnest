package com.foodnest.auth;

import com.foodnest.common.BadRequestException;
import com.foodnest.customer.Customer;
import com.foodnest.customer.CustomerRepository;
import com.foodnest.deliveryman.DeliveryMan;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.restaurant.Restaurant;
import com.foodnest.restaurant.RestaurantRepository;
import com.foodnest.security.CustomUserDetailsService;
import com.foodnest.security.UserPrincipal;
import com.foodnest.security.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryManRepository deliveryManRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(CustomerRepository customerRepository,
                       RestaurantRepository restaurantRepository,
                       DeliveryManRepository deliveryManRepository,
                       PasswordEncoder passwordEncoder,
                       CustomUserDetailsService userDetailsService) {
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryManRepository = deliveryManRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public UserSummaryResponse registerCustomer(CustomerRegisterRequest req) {
        if (customerRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("An account with email " + req.getEmail() + " already exists.");
        }
        if (customerRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestException("An account with phone " + req.getPhone() + " already exists.");
        }

        Customer customer = new Customer();
        customer.setFullName(req.getFullName());
        customer.setEmail(req.getEmail().toLowerCase().trim());
        customer.setPhone(req.getPhone().trim());
        customer.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        customer.setAddress(req.getAddress());

        Customer saved = customerRepository.save(customer);

        UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getFullName(), UserRole.CUSTOMER);
        response.setPhone(saved.getPhone());
        response.setAddress(saved.getAddress());
        return response;
    }

    @Transactional
    public UserSummaryResponse registerRestaurant(RestaurantRegisterRequest req) {
        if (restaurantRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("A restaurant account with email " + req.getEmail() + " already exists.");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(req.getName());
        restaurant.setOwnerName(req.getOwnerName());
        restaurant.setEmail(req.getEmail().toLowerCase().trim());
        restaurant.setPhone(req.getPhone().trim());
        restaurant.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        restaurant.setAddress(req.getAddress());
        restaurant.setCuisineType(req.getCuisineType() != null ? req.getCuisineType() : "Multi-Cuisine");
        restaurant.setImageUrl(req.getImageUrl());
        restaurant.setAccountStatus("ACTIVE");

        Restaurant saved = restaurantRepository.save(restaurant);

        UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getName(), UserRole.RESTAURANT);
        response.setPhone(saved.getPhone());
        response.setAddress(saved.getAddress());
        return response;
    }

    @Transactional
    public UserSummaryResponse registerDeliveryMan(DeliveryManRegisterRequest req) {
        if (deliveryManRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("A delivery partner account with email " + req.getEmail() + " already exists.");
        }
        if (deliveryManRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestException("A delivery partner account with phone " + req.getPhone() + " already exists.");
        }

        DeliveryMan deliveryMan = new DeliveryMan();
        deliveryMan.setFullName(req.getFullName());
        deliveryMan.setEmail(req.getEmail().toLowerCase().trim());
        deliveryMan.setPhone(req.getPhone().trim());
        deliveryMan.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        deliveryMan.setVehicleType(req.getVehicleType() != null ? req.getVehicleType() : "BIKE");
        deliveryMan.setIsAvailable(true);
        deliveryMan.setAccountStatus("ACTIVE");

        DeliveryMan saved = deliveryManRepository.save(deliveryMan);

        UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getFullName(), UserRole.DELIVERY_MAN);
        response.setPhone(saved.getPhone());
        return response;
    }

    public UserSummaryResponse login(UserRole expectedRole, String email, String password, HttpServletRequest request) {
        UserPrincipal principal = userDetailsService.loadByRoleAndEmail(expectedRole, email.toLowerCase().trim());

        if (!passwordEncoder.matches(password, principal.getPassword())) {
            throw new BadCredentialsException("Invalid password provided.");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        UserSummaryResponse response = new UserSummaryResponse(
                principal.getId(), principal.getUsername(), principal.getFullName(), principal.getRole());

        // Fill extra profile details
        if (expectedRole == UserRole.CUSTOMER) {
            customerRepository.findById(principal.getId()).ifPresent(c -> {
                response.setPhone(c.getPhone());
                response.setAddress(c.getAddress());
            });
        } else if (expectedRole == UserRole.RESTAURANT) {
            restaurantRepository.findById(principal.getId()).ifPresent(r -> {
                response.setPhone(r.getPhone());
                response.setAddress(r.getAddress());
            });
        } else if (expectedRole == UserRole.DELIVERY_MAN) {
            deliveryManRepository.findById(principal.getId()).ifPresent(d -> {
                response.setPhone(d.getPhone());
            });
        }

        return response;
    }

    public UserSummaryResponse getAuthenticatedUser(UserPrincipal principal) {
        UserSummaryResponse response = new UserSummaryResponse(
                principal.getId(), principal.getUsername(), principal.getFullName(), principal.getRole());

        if (principal.getRole() == UserRole.CUSTOMER) {
            customerRepository.findById(principal.getId()).ifPresent(c -> {
                response.setPhone(c.getPhone());
                response.setAddress(c.getAddress());
            });
        } else if (principal.getRole() == UserRole.RESTAURANT) {
            restaurantRepository.findById(principal.getId()).ifPresent(r -> {
                response.setPhone(r.getPhone());
                response.setAddress(r.getAddress());
            });
        } else if (principal.getRole() == UserRole.DELIVERY_MAN) {
            deliveryManRepository.findById(principal.getId()).ifPresent(d -> {
                response.setPhone(d.getPhone());
            });
        }

        return response;
    }
}
