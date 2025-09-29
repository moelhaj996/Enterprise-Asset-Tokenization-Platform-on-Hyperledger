package com.enterprise.tokenization.controller;

import com.enterprise.tokenization.dto.AuthRequest;
import com.enterprise.tokenization.dto.AuthResponse;
import com.enterprise.tokenization.dto.ErrorResponse;
import com.enterprise.tokenization.dto.RegisterRequest;
import com.enterprise.tokenization.model.User;
import com.enterprise.tokenization.model.UserRole;
import com.enterprise.tokenization.repository.UserRepository;
import com.enterprise.tokenization.security.CustomUserDetailsService;
import com.enterprise.tokenization.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * REST controller for authentication and user registration operations.
 * Provides endpoints for user login, registration, and retrieving current user information.
 *
 * <p>This controller handles JWT-based authentication, allowing users to log in with
 * credentials and receive a JWT token for subsequent authenticated requests. It also
 * provides user registration functionality with password encryption and role assignment.</p>
 *
 * @author Enterprise Tokenization Platform
 * @version 1.0
 * @since 2025-09-29
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
@Validated
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>Validates the provided username and password credentials. If authentication
     * is successful, generates and returns a JWT token that can be used for
     * authenticated requests.</p>
     *
     * @param authRequest the authentication request containing username and password
     * @return ResponseEntity containing AuthResponse with JWT token and user details
     */
    @Operation(
        summary = "User login",
        description = "Authenticates user credentials and returns a JWT token for authenticated requests"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request format",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login attempt for user: {}", authRequest.getUsername());

        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getUsername(),
                    authRequest.getPassword()
                )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(authentication);
            log.info("User {} authenticated successfully", authRequest.getUsername());

            // Get user details for response
            User user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

            // Build and return response
            AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000) // Convert to seconds
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", authRequest.getUsername());
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid username or password")
                .path("/api/auth/login")
                .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", authRequest.getUsername(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentication failed: " + e.getMessage())
                .path("/api/auth/login")
                .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", authRequest.getUsername(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred during login")
                .path("/api/auth/login")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Registers a new user in the system.
     *
     * <p>Creates a new user account with the provided credentials, Ethereum address,
     * and role. The password is encrypted using BCrypt before storage. Validates that
     * the username is unique and the Ethereum address format is correct.</p>
     *
     * @param registerRequest the registration request containing user details
     * @return ResponseEntity containing AuthResponse with JWT token for the new user
     */
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account with encrypted password and returns a JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User successfully registered",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or username already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Username or Ethereum address already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for username: {}", registerRequest.getUsername());

        try {
            // Check if username already exists
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                log.warn("Registration failed: Username already exists - {}", registerRequest.getUsername());
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CONFLICT.value())
                    .error("Conflict")
                    .message("Username already exists: " + registerRequest.getUsername())
                    .path("/api/auth/register")
                    .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Check if Ethereum address already exists
            if (userRepository.findByEthereumAddress(registerRequest.getEthereumAddress()).isPresent()) {
                log.warn("Registration failed: Ethereum address already exists - {}",
                    registerRequest.getEthereumAddress());
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CONFLICT.value())
                    .error("Conflict")
                    .message("Ethereum address already registered")
                    .path("/api/auth/register")
                    .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Validate and parse role
            UserRole role;
            try {
                role = UserRole.valueOf(registerRequest.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid role provided: {}", registerRequest.getRole());
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message("Invalid role: " + registerRequest.getRole())
                    .path("/api/auth/register")
                    .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Create new user with encrypted password
            User newUser = User.builder()
                .username(registerRequest.getUsername())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .ethereumAddress(registerRequest.getEthereumAddress())
                .role(role)
                .enabled(true)
                .build();

            // Save user to database
            newUser = userRepository.save(newUser);
            log.info("User registered successfully: {} with role: {}", newUser.getUsername(), newUser.getRole());

            // Authenticate the newly created user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    registerRequest.getUsername(),
                    registerRequest.getPassword()
                )
            );

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(authentication);

            // Build and return response
            AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000) // Convert to seconds
                .username(newUser.getUsername())
                .role(newUser.getRole().name())
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during registration for user: {}",
                registerRequest.getUsername(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to register user: " + e.getMessage())
                .path("/api/auth/register")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves information about the currently authenticated user.
     *
     * <p>Returns the username, role, and Ethereum address of the user
     * making the request. Requires a valid JWT token.</p>
     *
     * @return ResponseEntity containing current user information
     */
    @Operation(
        summary = "Get current user",
        description = "Returns information about the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user information",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = User.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getCurrentUser() {
        try {
            // Get the authenticated user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            log.debug("Fetching current user info for: {}", username);

            // Retrieve user from database
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

            log.debug("Successfully retrieved user info for: {}", username);

            // Return user information (excluding password hash)
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            log.error("Error retrieving current user information", e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve user information: " + e.getMessage())
                .path("/api/auth/me")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Exception handler for validation errors.
     *
     * @param ex the validation exception
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        log.error("Validation error: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(errorMessage)
            .path("/api/auth")
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Exception handler for general exceptions.
     *
     * @param ex the exception
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error in AuthController", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path("/api/auth")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}