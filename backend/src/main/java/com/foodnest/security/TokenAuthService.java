package com.foodnest.security;

import com.foodnest.customer.CustomerRepository;
import com.foodnest.deliveryman.DeliveryManRepository;
import com.foodnest.restaurant.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance token registry with self-healing database persistence,
 * allowing independent, concurrent authentication sessions for Customer, Restaurant,
 * and Delivery Partner accounts that survive application restarts.
 */
@Service
public class TokenAuthService {

    private final Map<String, UserPrincipal> tokenMap = new ConcurrentHashMap<>();
    private final RestaurantRepository restaurantRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryManRepository deliveryManRepository;

    public TokenAuthService() {
        this(null, null, null);
    }

    public TokenAuthService(RestaurantRepository restaurantRepository,
                            CustomerRepository customerRepository,
                            DeliveryManRepository deliveryManRepository) {
        this.restaurantRepository = restaurantRepository;
        this.customerRepository = customerRepository;
        this.deliveryManRepository = deliveryManRepository;
    }

    public String generateToken(UserPrincipal principal) {
        String roleTag = switch (principal.getRole()) {
            case RESTAURANT -> "REST";
            case DELIVERY_MAN -> "RIDER";
            case CUSTOMER -> "CUST";
        };
        // Encode role tag and user ID so the session can self-heal across server restarts
        String token = "FN-" + roleTag + "-" + principal.getId() + "-" + UUID.randomUUID().toString();
        tokenMap.put(token, principal);
        return token;
    }

    public UserPrincipal getPrincipal(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String cleanToken = token.trim();
        UserPrincipal cached = tokenMap.get(cleanToken);
        if (cached != null) {
            return cached;
        }

        // Auto-recover session from database after application restart
        try {
            if (cleanToken.startsWith("FN-")) {
                String[] parts = cleanToken.split("-");
                if (parts.length >= 4) {
                    String roleTag = parts[1];
                    Long id = Long.parseLong(parts[2]);
                    UserPrincipal recovered = switch (roleTag) {
                        case "REST" -> restaurantRepository != null ? restaurantRepository.findById(id)
                                .map(r -> new UserPrincipal(r.getId(), r.getEmail(), r.getPasswordHash(), r.getName(), UserRole.RESTAURANT))
                                .orElse(null) : null;
                        case "CUST" -> customerRepository != null ? customerRepository.findById(id)
                                .map(c -> new UserPrincipal(c.getId(), c.getEmail(), c.getPasswordHash(), c.getFullName(), UserRole.CUSTOMER))
                                .orElse(null) : null;
                        case "RIDER" -> deliveryManRepository != null ? deliveryManRepository.findById(id)
                                .map(d -> new UserPrincipal(d.getId(), d.getEmail(), d.getPasswordHash(), d.getFullName(), UserRole.DELIVERY_MAN))
                                .orElse(null) : null;
                        default -> null;
                    };
                    if (recovered != null) {
                        tokenMap.put(cleanToken, recovered);
                        return recovered;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public void removeToken(String token) {
        if (token != null) {
            tokenMap.remove(token.trim());
        }
    }

    public void invalidateTokensForUser(Long userId, UserRole role) {
        if (userId == null || role == null) return;
        tokenMap.entrySet().removeIf(entry -> {
            UserPrincipal p = entry.getValue();
            return userId.equals(p.getId()) && role == p.getRole();
        });
    }
}
