package com.foodnest;

import com.foodnest.auth.AuthService;
import com.foodnest.auth.CustomerRegisterRequest;
import com.foodnest.auth.UserSummaryResponse;
import com.foodnest.common.BadRequestException;
import com.foodnest.customer.Customer;
import com.foodnest.customer.CustomerRepository;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.restaurant.RestaurantRepository;
import com.foodnest.security.CustomUserDetailsService;
import com.foodnest.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private DeliveryManRepository deliveryManRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CustomUserDetailsService userDetailsService;
    private AuthService authService;

    private CustomerRegisterRequest registerRequest;

    @BeforeEach
    public void setup() {
        userDetailsService = new CustomUserDetailsService(customerRepository, restaurantRepository, deliveryManRepository);
        authService = new AuthService(customerRepository, restaurantRepository, deliveryManRepository, passwordEncoder, userDetailsService);

        registerRequest = new CustomerRegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPhone("9123456789");
        registerRequest.setPassword("secret123");
        registerRequest.setAddress("123 Maple Street");
    }

    @Test
    public void testSuccessfulCustomerRegistration() {
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("9123456789")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashedPassword");

        Customer savedCustomer = new Customer();
        savedCustomer.setId(100L);
        savedCustomer.setFullName("John Doe");
        savedCustomer.setEmail("john@example.com");
        savedCustomer.setPhone("9123456789");
        savedCustomer.setPasswordHash("hashedPassword");

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        UserSummaryResponse response = authService.registerCustomer(registerRequest);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(UserRole.CUSTOMER, response.getRole());
        verify(passwordEncoder).encode("secret123");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    public void testDuplicateEmailRegistrationRejection() {
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerCustomer(registerRequest));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    public void testDuplicatePhoneRegistrationRejection() {
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("9123456789")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.registerCustomer(registerRequest));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(customerRepository, never()).save(any(Customer.class));
    }
}
