package com.foodnest.config;

import com.foodnest.security.RestAccessDeniedHandler;
import com.foodnest.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final com.foodnest.security.TokenAuthenticationFilter tokenAuthenticationFilter;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          com.foodnest.security.TokenAuthenticationFilter tokenAuthenticationFilter) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(tokenAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                    "/api/**",
                    "/ws/**",
                    "/ws-raw/**"
                )
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Public Authentication Endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Public Restaurant Browsing & Cart Calculations
                .requestMatchers(HttpMethod.GET, "/api/restaurants", "/api/restaurants/**").permitAll()
                .requestMatchers("/api/cart/**").permitAll()

                // Customer endpoints strictly restricted to CUSTOMER role
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel").hasRole("CUSTOMER")

                // Restaurant-specific endpoints
                .requestMatchers("/api/restaurant/**").hasRole("RESTAURANT")

                // Delivery-man-specific endpoints
                .requestMatchers("/api/deliveryman/**").hasRole("DELIVERY_MAN")

                // Order details & Tracking (ownership verified in service)
                .requestMatchers(HttpMethod.GET, "/api/orders/*").authenticated()
                .requestMatchers("/api/tracking/**").authenticated()

                // WebSocket
                .requestMatchers("/ws/**", "/ws-raw/**").permitAll()

                // Static Web Assets
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.ico",
                    "/customer/**",
                    "/restaurant/**",
                    "/deliveryman/**",
                    "/shared/**"
                ).permitAll()

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .maximumSessions(2)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        java.util.List<String> origins = new java.util.ArrayList<>(Arrays.asList(
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "https://*.railway.app",
            "https://*.vercel.app",
            "https://*.netlify.app",
            "https://*.onrender.com",
            "https://*.pages.dev",
            "https://*.github.io"
        ));
        String envCors = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envCors != null && !envCors.isBlank()) {
            for (String orig : envCors.split(",")) {
                String trimmed = orig.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-XSRF-TOKEN",
            "X-Auth-Token"
        ));
        configuration.setExposedHeaders(List.of("X-XSRF-TOKEN", "X-Auth-Token", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
