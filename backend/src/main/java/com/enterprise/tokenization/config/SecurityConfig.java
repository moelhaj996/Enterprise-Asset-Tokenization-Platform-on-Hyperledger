package com.enterprise.tokenization.config;

import com.enterprise.tokenization.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the Enterprise Asset Tokenization Platform.
 * Configures JWT-based authentication, authorization, and CORS policies.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configures the security filter chain with JWT authentication and role-based access control.
     *
     * @param http HttpSecurity object to configure
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
                // Disable CSRF for stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/blockchain/status").permitAll()

                        // Swagger/OpenAPI documentation endpoints
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Actuator health endpoint
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Asset minting - requires MINTER or ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/assets/mint").hasAnyRole("MINTER", "ADMIN")

                        // Asset burning - requires BURNER or ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/assets/burn").hasAnyRole("BURNER", "ADMIN")

                        // Asset transfer - requires ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/assets/transfer").hasRole("ADMIN")

                        // All other asset endpoints - require authentication
                        .requestMatchers("/api/assets/**").authenticated()

                        // User management - ADMIN only
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Compliance endpoints - requires COMPLIANCE_OFFICER or ADMIN
                        .requestMatchers("/api/compliance/**").hasAnyRole("COMPLIANCE_OFFICER", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Configure session management - stateless for JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security filter chain configured successfully");
        return http.build();
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     * Allows requests from specified origins with common HTTP methods.
     *
     * @return CorsConfigurationSource with CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS policy");

        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins (configure based on your frontend deployment)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",  // React development
                "http://localhost:4200",  // Angular development
                "http://localhost:8080"   // Same origin
        ));

        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow common headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Expose headers that the frontend can access
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configures the authentication provider using UserDetailsService and PasswordEncoder.
     *
     * @return Configured AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        log.info("Configuring authentication provider");

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * Provides the AuthenticationManager bean.
     *
     * @param config AuthenticationConfiguration
     * @return AuthenticationManager instance
     * @throws Exception if getting authentication manager fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.info("Creating authentication manager");
        return config.getAuthenticationManager();
    }

    /**
     * Provides BCrypt password encoder for secure password hashing.
     * Uses default strength of 10 rounds.
     *
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Configuring BCrypt password encoder");
        return new BCryptPasswordEncoder();
    }
}