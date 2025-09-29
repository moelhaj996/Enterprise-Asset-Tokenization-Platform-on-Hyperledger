package com.enterprise.tokenization.service;

import com.enterprise.tokenization.dto.AuthRequest;
import com.enterprise.tokenization.dto.AuthResponse;
import com.enterprise.tokenization.dto.RegisterRequest;
import com.enterprise.tokenization.model.User;
import com.enterprise.tokenization.model.UserRole;
import com.enterprise.tokenization.repository.UserRepository;
import com.enterprise.tokenization.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for authentication and user management operations.
 * Handles user registration, login, and JWT token generation.
 *
 * @author Enterprise Tokenization Platform
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private final AuthenticationManager authenticationManager;

    @Autowired
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param request the authentication request containing username and password
     * @return AuthResponse containing JWT token and user details
     * @throws BadCredentialsException if authentication fails
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        try {
            log.info("Processing login request for username: {}", request.getUsername());

            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);

            // Retrieve user details
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found after successful authentication: {}", request.getUsername());
                    return new UsernameNotFoundException("User not found: " + request.getUsername());
                });

            log.info("User {} logged in successfully with role: {}", user.getUsername(), user.getRole());

            // Build and return response
            return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000) // Convert to seconds
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();

        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for username: {}", request.getUsername());
            throw new BadCredentialsException("Invalid username or password", e);
        } catch (Exception e) {
            log.error("Error during login for username: {}", request.getUsername(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a new user with encrypted password and generates a JWT token.
     * Validates that username and email are not already in use.
     *
     * @param request the registration request containing user details
     * @return AuthResponse containing JWT token and user details
     * @throws IllegalArgumentException if username or ethereum address already exists
     * @throws RuntimeException if registration fails
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            log.info("Processing registration request for username: {}", request.getUsername());

            // Validate duplicate username
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("Registration failed: Username {} already exists", request.getUsername());
                throw new IllegalArgumentException("Username already exists: " + request.getUsername());
            }

            // Validate duplicate ethereum address
            if (userRepository.findByEthereumAddress(request.getEthereumAddress()).isPresent()) {
                log.warn("Registration failed: Ethereum address {} already in use", request.getEthereumAddress());
                throw new IllegalArgumentException("Ethereum address already in use: " + request.getEthereumAddress());
            }

            // Parse and validate role
            UserRole role;
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid role provided: {}", request.getRole());
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }

            // Create new user with BCrypt encrypted password
            User newUser = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .ethereumAddress(request.getEthereumAddress())
                .role(role)
                .enabled(true)
                .build();

            // Save user to database
            User savedUser = userRepository.save(newUser);
            log.info("User {} registered successfully with role: {}", savedUser.getUsername(), savedUser.getRole());

            // Authenticate the newly registered user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);

            log.info("JWT token generated for new user: {}", savedUser.getUsername());

            // Build and return response
            return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000) // Convert to seconds
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .build();

        } catch (IllegalArgumentException e) {
            log.error("Validation error during registration: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error during registration for username: {}", request.getUsername(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the currently authenticated user from the security context.
     *
     * @return the currently authenticated User entity
     * @throws UsernameNotFoundException if the authenticated user is not found in database
     * @throws RuntimeException if no authentication is present in security context
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        try {
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("No authenticated user found in security context");
                throw new RuntimeException("No authenticated user found");
            }

            String username = authentication.getName();
            log.debug("Retrieving current user: {}", username);

            // Retrieve user from database
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

            log.debug("Current user retrieved successfully: {}", user.getUsername());
            return user;

        } catch (UsernameNotFoundException e) {
            log.error("User not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving current user", e);
            throw new RuntimeException("Failed to retrieve current user: " + e.getMessage(), e);
        }
    }
}