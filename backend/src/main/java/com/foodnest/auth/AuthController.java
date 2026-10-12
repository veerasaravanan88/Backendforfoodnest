package com.foodnest.auth;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import com.foodnest.security.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        UserSummaryResponse response = authService.registerCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer registered successfully", response));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> loginCustomer(@Valid @RequestBody LoginRequest request,
                                                                         HttpServletRequest httpRequest) {
        UserSummaryResponse response = authService.login(UserRole.CUSTOMER, request.getEmail(), request.getPassword(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully as Customer", response));
    }

    @PostMapping("/restaurant/register")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> registerRestaurant(@Valid @RequestBody RestaurantRegisterRequest request) {
        UserSummaryResponse response = authService.registerRestaurant(request);
        return ResponseEntity.ok(ApiResponse.success("Restaurant registered successfully", response));
    }

    @PostMapping("/restaurant/login")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> loginRestaurant(@Valid @RequestBody LoginRequest request,
                                                                           HttpServletRequest httpRequest) {
        UserSummaryResponse response = authService.login(UserRole.RESTAURANT, request.getEmail(), request.getPassword(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully as Restaurant Partner", response));
    }

    @PostMapping("/deliveryman/register")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> registerDeliveryMan(@Valid @RequestBody DeliveryManRegisterRequest request) {
        UserSummaryResponse response = authService.registerDeliveryMan(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery partner registered successfully", response));
    }

    @PostMapping("/deliveryman/login")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> loginDeliveryMan(@Valid @RequestBody LoginRequest request,
                                                                            HttpServletRequest httpRequest) {
        UserSummaryResponse response = authService.login(UserRole.DELIVERY_MAN, request.getEmail(), request.getPassword(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully as Delivery Partner", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        UserSummaryResponse response = authService.getAuthenticatedUser(principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "token", token.getToken(),
                    "headerName", token.getHeaderName(),
                    "parameterName", token.getParameterName()
            )));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "No CSRF token generated")));
    }
}
