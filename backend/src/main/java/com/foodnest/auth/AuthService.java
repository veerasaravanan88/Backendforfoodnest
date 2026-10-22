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
    private final com.foodnest.security.TokenAuthService tokenAuthService;

    public AuthService(CustomerRepository customerRepository,
                       RestaurantRepository restaurantRepository,
                       DeliveryManRepository deliveryManRepository,
                       PasswordEncoder passwordEncoder,
                       CustomUserDetailsService userDetailsService) {
        this(customerRepository, restaurantRepository, deliveryManRepository, passwordEncoder, userDetailsService, new com.foodnest.security.TokenAuthService(restaurantRepository, customerRepository, deliveryManRepository));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(CustomerRepository customerRepository,
                       RestaurantRepository restaurantRepository,
                       DeliveryManRepository deliveryManRepository,
                       PasswordEncoder passwordEncoder,
                       CustomUserDetailsService userDetailsService,
                       com.foodnest.security.TokenAuthService tokenAuthService) {
        this.customerRepository = customerRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryManRepository = deliveryManRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.tokenAuthService = tokenAuthService;
    }

    private void establishSession(UserPrincipal principal, HttpServletRequest request) {
        if (request != null) {
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                try {
                    existingSession.invalidate();
                } catch (Exception ignored) {
                }
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }
    }

    @Transactional
    public UserSummaryResponse registerCustomer(CustomerRegisterRequest req) {
        return registerCustomer(req, null);
    }

    @Transactional
    public UserSummaryResponse registerCustomer(CustomerRegisterRequest req, HttpServletRequest request) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();
        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with email " + req.getEmail() + " already exists.");
        }
        String phone = req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : null;
        if (phone != null && customerRepository.existsByPhone(phone)) {
            throw new BadRequestException("An account with phone " + phone + " already exists.");
        }

        Customer customer = new Customer();
        customer.setFullName(req.getFullName());
        customer.setEmail(normalizedEmail);
        customer.setPhone(phone != null ? phone : "98" + (long)(Math.random() * 90000000L + 10000000L));
        customer.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        customer.setAddress(req.getAddress() != null ? req.getAddress().trim() : "123 Food Street, City");

        Customer saved = customerRepository.save(customer);

        UserPrincipal principal = new UserPrincipal(saved.getId(), saved.getEmail(), saved.getPasswordHash(), saved.getFullName(), UserRole.CUSTOMER);
        establishSession(principal, request);

        String token = tokenAuthService.generateToken(principal);
        UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getFullName(), UserRole.CUSTOMER);
        response.setPhone(saved.getPhone());
        response.setAddress(saved.getAddress());
        response.setToken(token);
        return response;
    }

    @Transactional
    public UserSummaryResponse registerRestaurant(RestaurantRegisterRequest req) {
        return registerRestaurant(req, null);
    }

    @Transactional
    public UserSummaryResponse registerRestaurant(RestaurantRegisterRequest req, HttpServletRequest request) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();
        java.util.Optional<Restaurant> existingOpt = restaurantRepository.findByEmail(normalizedEmail);
        Restaurant restaurant;
        if (existingOpt.isPresent()) {
            restaurant = existingOpt.get();
            if (req.getName() != null && !req.getName().isBlank()) restaurant.setName(req.getName().trim());
            if (req.getOwnerName() != null && !req.getOwnerName().isBlank()) restaurant.setOwnerName(req.getOwnerName().trim());
            if (req.getPhone() != null && !req.getPhone().isBlank()) restaurant.setPhone(req.getPhone().trim());
            if (req.getAddress() != null && !req.getAddress().isBlank()) restaurant.setAddress(req.getAddress().trim());
            if (req.getLatitude() != null) restaurant.setLatitude(req.getLatitude());
            if (req.getLongitude() != null) restaurant.setLongitude(req.getLongitude());
            if (req.getCuisineType() != null && !req.getCuisineType().isBlank()) restaurant.setCuisineType(req.getCuisineType().trim());
            if (req.getImageUrl() != null && !req.getImageUrl().isBlank()) restaurant.setImageUrl(req.getImageUrl().trim());
            restaurant.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            restaurant.setAccountStatus("ACTIVE");
        } else {
            restaurant = new Restaurant();
            restaurant.setName(req.getName());
            restaurant.setOwnerName(req.getOwnerName());
            restaurant.setEmail(normalizedEmail);
            restaurant.setPhone(req.getPhone().trim());
            restaurant.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            restaurant.setAddress(req.getAddress());
            restaurant.setLatitude(req.getLatitude());
            restaurant.setLongitude(req.getLongitude());
            restaurant.setCuisineType(req.getCuisineType() != null ? req.getCuisineType() : "Multi-Cuisine");
            restaurant.setImageUrl(req.getImageUrl() != null && !req.getImageUrl().isBlank() ? req.getImageUrl() : "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800");
            restaurant.setAccountStatus("ACTIVE");
        }

        Restaurant saved = restaurantRepository.save(restaurant);

        UserPrincipal principal = new UserPrincipal(saved.getId(), saved.getEmail(), saved.getPasswordHash(), saved.getName(), UserRole.RESTAURANT);
        establishSession(principal, request);

        String token = tokenAuthService.generateToken(principal);
        UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getName(), UserRole.RESTAURANT);
        response.setPhone(saved.getPhone());
        response.setAddress(saved.getAddress());
        response.setToken(token);
        return response;
    }

    @Transactional
    public UserSummaryResponse registerDeliveryMan(DeliveryManRegisterRequest req) {
        return registerDeliveryMan(req, null);
    }

    @Transactional
    public UserSummaryResponse registerDeliveryMan(DeliveryManRegisterRequest req, HttpServletRequest request) {
        String normalizedEmail = req.getEmail().toLowerCase().trim();
        java.util.Optional<DeliveryMan> existingOpt = deliveryManRepository.findByEmail(normalizedEmail);
        DeliveryMan deliveryMan;
        if (existingOpt.isPresent()) {
            deliveryMan = existingOpt.get();
            if (req.getFullName() != null && !req.getFullName().isBlank()) deliveryMan.setFullName(req.getFullName().trim());
            if (req.getPhone() != null && !req.getPhone().isBlank()) deliveryMan.setPhone(req.getPhone().trim());
            if (req.getVehicleType() != null && !req.getVehicleType().isBlank()) deliveryMan.setVehicleType(req.getVehicleType().trim());
            deliveryMan.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            deliveryMan.setIsAvailable(true);
            deliveryMan.setAccountStatus("ACTIVE");
        } else {
            deliveryMan = new DeliveryMan();
            deliveryMan.setFullName(req.getFullName());
            deliveryMan.setEmail(normalizedEmail);
            String phone = req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : "9876543290";
            if (deliveryManRepository.existsByPhone(phone)) {
                phone = phone.length() > 8 ? phone.substring(0, 8) + (int)(Math.random() * 90 + 10) : phone + (int)(Math.random() * 90 + 10);
            }
            deliveryMan.setPhone(phone);
            deliveryMan.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            deliveryMan.setVehicleType(req.getVehicleType() != null ? req.getVehicleType() : "BIKE");
            deliveryMan.setIsAvailable(true);
            deliveryMan.setAccountStatus("ACTIVE");
        }

        DeliveryMan saved = deliveryManRepository.save(deliveryMan);

        UserPrincipal principal = new UserPrincipal(saved.getId(), saved.getEmail(), saved.getPasswordHash(), saved.getFullName(), UserRole.DELIVERY_MAN);
        establishSession(principal, request);

        String token = tokenAuthService.generateToken(principal);
        UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getFullName(), UserRole.DELIVERY_MAN);
        response.setPhone(saved.getPhone());
        response.setToken(token);
        return response;
    }

    public UserSummaryResponse login(UserRole expectedRole, String email, String password, HttpServletRequest request) {
        String normalizedEmail = email.toLowerCase().trim();

        // Swiggy-Style Customer Login:
        // If a customer enters an email that does not exist in the database,
        // automatically create their account using the entered password and sign them in immediately!
        if (expectedRole == UserRole.CUSTOMER && !customerRepository.existsByEmail(normalizedEmail)) {
            Customer newCustomer = new Customer();
            String prefix = normalizedEmail.contains("@") ? normalizedEmail.substring(0, normalizedEmail.indexOf('@')) : normalizedEmail;
            String derivedName = !prefix.isEmpty() ? Character.toUpperCase(prefix.charAt(0)) + (prefix.length() > 1 ? prefix.substring(1) : "") : "Customer";
            newCustomer.setFullName(derivedName);
            newCustomer.setEmail(normalizedEmail);
            newCustomer.setPhone("98" + (long)(Math.random() * 90000000L + 10000000L));
            newCustomer.setPasswordHash(passwordEncoder.encode(password));
            newCustomer.setAddress("Customer Delivery Address");
            Customer saved = customerRepository.save(newCustomer);

            UserPrincipal principal = new UserPrincipal(saved.getId(), saved.getEmail(), saved.getPasswordHash(), saved.getFullName(), UserRole.CUSTOMER);
            establishSession(principal, request);

            String token = tokenAuthService.generateToken(principal);
            UserSummaryResponse response = new UserSummaryResponse(saved.getId(), saved.getEmail(), saved.getFullName(), UserRole.CUSTOMER);
            response.setPhone(saved.getPhone());
            response.setAddress(saved.getAddress());
            response.setToken(token);
            return response;
        }

        UserPrincipal principal = userDetailsService.loadByRoleAndEmail(expectedRole, normalizedEmail);

        if (!passwordEncoder.matches(password, principal.getPassword())) {
            throw new BadCredentialsException("Invalid password provided for " + email + ". Please check your password.");
        }

        establishSession(principal, request);

        String token = tokenAuthService.generateToken(principal);
        UserSummaryResponse response = new UserSummaryResponse(
                principal.getId(), principal.getUsername(), principal.getFullName(), principal.getRole());
        response.setToken(token);

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
                d.setLastActiveAt(java.time.LocalDateTime.now());
                deliveryManRepository.save(d);
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
