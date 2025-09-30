package com.enterprise.tokenization.controller;

import com.enterprise.tokenization.dto.*;
import com.enterprise.tokenization.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for asset management operations.
 * Provides endpoints for minting, burning, querying, and managing tokenized assets.
 *
 * <p>This controller handles all asset-related operations including creation (minting),
 * destruction (burning), balance queries, and transaction history. It integrates with
 * the AssetService layer to perform business logic and blockchain interactions.</p>
 *
 * @author Enterprise Tokenization Platform
 * @version 1.0
 * @since 2025-09-29
 */
@Slf4j
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Asset Management", description = "Endpoints for managing tokenized assets")
@Validated
public class AssetController {

    @Autowired
    private AssetService assetService;

    /**
     * Mints new asset tokens.
     *
     * <p>Creates new tokens for a specified asset. If the asset doesn't exist, it will
     * be created. If it exists, the supply will be increased. This operation requires
     * MINTER or ADMIN role.</p>
     *
     * @param request the asset minting request containing asset details and amount
     * @return ResponseEntity containing AssetResponse with minted asset details
     */
    @Operation(
        summary = "Mint new asset tokens",
        description = "Creates new tokens for an asset or increases the supply of an existing asset. Requires MINTER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Asset successfully minted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssetResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
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
            responseCode = "403",
            description = "User does not have required role (MINTER or ADMIN)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or blockchain transaction failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/mint")
    @PreAuthorize("hasAnyRole('MINTER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> mintAsset(
            @Valid @RequestBody
            @Parameter(description = "Asset minting request", required = true)
            AssetMintRequest request) {

        log.info("Received mint request for asset: {}", request.getAssetId());

        try {
            AssetResponse response = assetService.mintAsset(request);
            log.info("Asset minted successfully: {}", request.getAssetId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid mint request: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path("/api/assets/mint")
                .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to mint asset: {}", request.getAssetId(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to mint asset: " + e.getMessage())
                .path("/api/assets/mint")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Burns (destroys) asset tokens.
     *
     * <p>Reduces the supply of an asset by burning tokens from a specific address.
     * This operation is irreversible and requires BURNER or ADMIN role.</p>
     *
     * @param request the asset burn request containing asset ID, amount, and address
     * @return ResponseEntity containing TransactionResponse with burn transaction details
     */
    @Operation(
        summary = "Burn asset tokens",
        description = "Destroys tokens from a specific address, reducing the total supply. Requires BURNER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Asset successfully burned",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters or insufficient balance",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
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
            responseCode = "403",
            description = "User does not have required role (BURNER or ADMIN)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Asset not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or blockchain transaction failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/burn")
    @PreAuthorize("hasAnyRole('BURNER', 'ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> burnAsset(
            @Valid @RequestBody
            @Parameter(description = "Asset burn request", required = true)
            AssetBurnRequest request) {

        log.info("Received burn request for asset: {} with amount: {}",
            request.getAssetId(), request.getAmount());

        try {
            TransactionResponse response = assetService.burnAsset(request);
            log.info("Asset burned successfully: {}", request.getAssetId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid burn request: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path("/api/assets/burn")
                .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to burn asset: {}", request.getAssetId(), e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to burn asset: " + e.getMessage())
                .path("/api/assets/burn")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all assets in the system.
     *
     * <p>Returns a list of all tokenized assets that have been created.
     * Requires authentication.</p>
     *
     * @return ResponseEntity containing a list of AssetResponse objects
     */
    @Operation(
        summary = "Get all assets",
        description = "Returns a list of all tokenized assets in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved assets",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssetResponse.class)
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
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getAllAssets() {
        log.info("Received request to get all assets");

        try {
            List<AssetResponse> assets = assetService.getAllAssets();
            log.info("Retrieved {} assets", assets.size());
            return ResponseEntity.ok(assets);

        } catch (Exception e) {
            log.error("Failed to retrieve assets", e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve assets: " + e.getMessage())
                .path("/api/assets/")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves a specific asset by its ID.
     *
     * <p>Returns detailed information about a single asset including its supply,
     * issuer, and metadata.</p>
     *
     * @param assetId the unique identifier of the asset
     * @return ResponseEntity containing AssetResponse with asset details
     */
    @Operation(
        summary = "Get asset by ID",
        description = "Returns detailed information about a specific asset"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved asset",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssetResponse.class)
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
            description = "Asset not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{assetId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getAssetById(
            @PathVariable
            @Parameter(description = "Asset ID", required = true, example = "BOND-001")
            String assetId) {

        log.info("Received request to get asset: {}", assetId);

        try {
            AssetResponse asset = assetService.getAsset(assetId);
            log.info("Successfully retrieved asset: {}", assetId);
            return ResponseEntity.ok(asset);

        } catch (IllegalArgumentException e) {
            log.error("Asset not found: {}", assetId);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(e.getMessage())
                .path("/api/assets/" + assetId)
                .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to retrieve asset: {}", assetId, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve asset: " + e.getMessage())
                .path("/api/assets/" + assetId)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all assets of a specific type.
     *
     * <p>Filters assets by their type (e.g., CORPORATE_BOND, INVOICE, SUPPLY_CHAIN)
     * and returns matching assets.</p>
     *
     * @param type the asset type to filter by
     * @return ResponseEntity containing a list of AssetResponse objects
     */
    @Operation(
        summary = "Get assets by type",
        description = "Returns all assets of a specific type (e.g., CORPORATE_BOND, INVOICE)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved assets",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssetResponse.class)
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
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/type/{type}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getAssetsByType(
            @PathVariable
            @Parameter(description = "Asset type", required = true, example = "CORPORATE_BOND")
            String type) {

        log.info("Received request to get assets by type: {}", type);

        try {
            List<AssetResponse> assets = assetService.getAssetsByType(type);
            log.info("Retrieved {} assets of type: {}", assets.size(), type);
            return ResponseEntity.ok(assets);

        } catch (Exception e) {
            log.error("Failed to retrieve assets by type: {}", type, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve assets by type: " + e.getMessage())
                .path("/api/assets/type/" + type)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves transaction history for a specific asset.
     *
     * <p>Returns all transactions (minting, burning, transfers) associated with
     * the specified asset, ordered by timestamp.</p>
     *
     * @param assetId the asset ID to get transactions for
     * @return ResponseEntity containing a list of TransactionResponse objects
     */
    @Operation(
        summary = "Get asset transaction history",
        description = "Returns all transactions associated with a specific asset"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transactions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponse.class)
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
            description = "Asset not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{assetId}/transactions")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getAssetTransactions(
            @PathVariable
            @Parameter(description = "Asset ID", required = true, example = "BOND-001")
            String assetId) {

        log.info("Received request to get transactions for asset: {}", assetId);

        try {
            List<TransactionResponse> transactions = assetService.getAssetTransactions(assetId);
            log.info("Retrieved {} transactions for asset: {}", transactions.size(), assetId);
            return ResponseEntity.ok(transactions);

        } catch (IllegalArgumentException e) {
            log.error("Asset not found: {}", assetId);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(e.getMessage())
                .path("/api/assets/" + assetId + "/transactions")
                .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to retrieve transactions for asset: {}", assetId, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve transactions: " + e.getMessage())
                .path("/api/assets/" + assetId + "/transactions")
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves the balance of an asset for a specific address.
     *
     * <p>Returns the amount of tokens held by the specified address for the given asset.
     * Queries the blockchain first, falls back to database if blockchain query fails.</p>
     *
     * @param assetId the asset ID
     * @param address the holder's Ethereum address
     * @return ResponseEntity containing the balance as BigInteger
     */
    @Operation(
        summary = "Get asset balance for address",
        description = "Returns the token balance of a specific address for a given asset"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved balance",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BigInteger.class)
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
            description = "Asset or address not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{assetId}/balance/{address}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getAssetBalance(
            @PathVariable
            @Parameter(description = "Asset ID", required = true, example = "BOND-001")
            String assetId,
            @PathVariable
            @Parameter(description = "Ethereum address", required = true,
                example = "0x1234567890123456789012345678901234567890")
            String address) {

        log.info("Received request to get balance for asset: {} and address: {}", assetId, address);

        try {
            BigInteger balance = assetService.getAssetBalance(assetId, address);
            log.info("Balance for asset {} and address {}: {}", assetId, address, balance);
            return ResponseEntity.ok(balance);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(e.getMessage())
                .path("/api/assets/" + assetId + "/balance/" + address)
                .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Failed to retrieve balance for asset: {} and address: {}",
                assetId, address, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve balance: " + e.getMessage())
                .path("/api/assets/" + assetId + "/balance/" + address)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all assets held by a specific address.
     *
     * <p>Returns a list of all assets where the specified address holds a non-zero balance.</p>
     *
     * @param address the holder's Ethereum address
     * @return ResponseEntity containing a list of AssetResponse objects
     */
    @Operation(
        summary = "Get assets held by address",
        description = "Returns all assets where the specified address holds tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved assets",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssetResponse.class)
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
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/holder/{address}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getAssetsByHolder(
            @PathVariable
            @Parameter(description = "Ethereum address", required = true,
                example = "0x1234567890123456789012345678901234567890")
            String address) {

        log.info("Received request to get assets for holder: {}", address);

        try {
            List<AssetResponse> assets = assetService.getHolderAssets(address);
            log.info("Retrieved {} assets for holder: {}", assets.size(), address);
            return ResponseEntity.ok(assets);

        } catch (Exception e) {
            log.error("Failed to retrieve assets for holder: {}", address, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve assets: " + e.getMessage())
                .path("/api/assets/holder/" + address)
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
            .path("/api/assets")
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
        log.error("Unexpected error in AssetController", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path("/api/assets")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}