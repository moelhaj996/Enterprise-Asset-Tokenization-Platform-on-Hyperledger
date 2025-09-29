package com.enterprise.tokenization.controller;

import com.enterprise.tokenization.blockchain.BlockchainService;
import com.enterprise.tokenization.dto.BlockchainStatusResponse;
import com.enterprise.tokenization.dto.ErrorResponse;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for blockchain network operations and monitoring.
 * Provides endpoints for querying blockchain status and transaction details.
 *
 * <p>This controller offers public endpoints for monitoring the health and status
 * of the blockchain network, as well as authenticated endpoints for querying
 * transaction details and receipts.</p>
 *
 * @author Enterprise Tokenization Platform
 * @version 1.0
 * @since 2025-09-29
 */
@Slf4j
@RestController
@RequestMapping("/api/blockchain")
@Tag(name = "Blockchain Operations", description = "Endpoints for blockchain network monitoring and transaction queries")
@Validated
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    /**
     * Retrieves the current blockchain network status.
     *
     * <p>Returns information about the blockchain network including chain ID,
     * latest block number, peer count, and synchronization status. This is a
     * public endpoint that doesn't require authentication, useful for health checks
     * and monitoring.</p>
     *
     * @return ResponseEntity containing BlockchainStatusResponse with network details
     */
    @Operation(
        summary = "Get blockchain network status",
        description = "Returns current status and health metrics of the blockchain network. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved network status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BlockchainStatusResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to connect to blockchain network",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Blockchain network is unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/status")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getNetworkStatus() {
        log.info("Received request to get blockchain network status");

        try {
            BlockchainStatusResponse status = blockchainService.getNetworkStatus();
            log.info("Successfully retrieved network status - Chain ID: {}, Block: {}",
                status.getChainId(), status.getBlockNumber());
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Failed to retrieve blockchain network status", e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Unable to connect to blockchain network: " + e.getMessage())
                .path("/api/blockchain/status")
                .build();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Retrieves detailed information about a specific transaction.
     *
     * <p>Returns the transaction receipt containing details such as block number,
     * gas used, status, and event logs. This endpoint requires authentication.</p>
     *
     * @param hash the transaction hash to query
     * @return ResponseEntity containing transaction details as a map
     */
    @Operation(
        summary = "Get transaction details",
        description = "Returns detailed information about a blockchain transaction including receipt, status, and logs"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transaction details",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid transaction hash format",
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
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or blockchain query failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/transaction/{hash}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getTransactionDetails(
            @PathVariable
            @Parameter(
                description = "Transaction hash",
                required = true,
                example = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
            )
            String hash) {

        log.info("Received request to get transaction details for hash: {}", hash);

        try {
            // Validate transaction hash format
            if (hash == null || hash.trim().isEmpty()) {
                log.error("Empty transaction hash provided");
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message("Transaction hash cannot be empty")
                    .path("/api/blockchain/transaction/" + hash)
                    .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate hash format (0x + 64 hex characters)
            if (!hash.matches("^0x[a-fA-F0-9]{64}$")) {
                log.error("Invalid transaction hash format: {}", hash);
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message("Invalid transaction hash format. Expected: 0x followed by 64 hexadecimal characters")
                    .path("/api/blockchain/transaction/" + hash)
                    .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Retrieve transaction receipt from blockchain
            TransactionReceipt receipt = blockchainService.getTransactionReceipt(hash);

            // Build response with transaction details
            Map<String, Object> transactionDetails = new HashMap<>();
            transactionDetails.put("transactionHash", receipt.getTransactionHash());
            transactionDetails.put("blockNumber", receipt.getBlockNumber());
            transactionDetails.put("blockHash", receipt.getBlockHash());
            transactionDetails.put("from", receipt.getFrom());
            transactionDetails.put("to", receipt.getTo());
            transactionDetails.put("contractAddress", receipt.getContractAddress());
            transactionDetails.put("gasUsed", receipt.getGasUsed());
            transactionDetails.put("cumulativeGasUsed", receipt.getCumulativeGasUsed());
            transactionDetails.put("status", receipt.getStatus());
            transactionDetails.put("statusOk", receipt.isStatusOK());
            transactionDetails.put("logs", receipt.getLogs());
            transactionDetails.put("logsBloom", receipt.getLogsBloom());
            transactionDetails.put("transactionIndex", receipt.getTransactionIndex());
            transactionDetails.put("effectiveGasPrice", receipt.getEffectiveGasPrice());
            transactionDetails.put("type", receipt.getType());

            log.info("Successfully retrieved transaction details for hash: {}", hash);
            return ResponseEntity.ok(transactionDetails);

        } catch (IllegalArgumentException e) {
            log.error("Invalid transaction hash: {}", hash, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid transaction hash: " + e.getMessage())
                .path("/api/blockchain/transaction/" + hash)
                .build();
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // Check if transaction not found
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                log.error("Transaction not found: {}", hash);
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.NOT_FOUND.value())
                    .error("Not Found")
                    .message("Transaction not found with hash: " + hash)
                    .path("/api/blockchain/transaction/" + hash)
                    .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            log.error("Failed to retrieve transaction details for hash: {}", hash, e);
            ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Failed to retrieve transaction details: " + e.getMessage())
                .path("/api/blockchain/transaction/" + hash)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for the blockchain connection.
     *
     * <p>Performs a simple connectivity test to verify the blockchain network
     * is accessible and responding. Returns a lightweight response suitable
     * for load balancer health checks.</p>
     *
     * @return ResponseEntity with health status
     */
    @Operation(
        summary = "Blockchain health check",
        description = "Lightweight endpoint to verify blockchain connectivity. Suitable for load balancer health checks."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Blockchain is healthy and accessible",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Blockchain is unhealthy or not accessible",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/health")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> healthCheck() {
        log.debug("Received blockchain health check request");

        try {
            // Perform lightweight health check by getting block number
            BlockchainStatusResponse status = blockchainService.getNetworkStatus();

            Map<String, Object> healthResponse = new HashMap<>();
            healthResponse.put("status", "UP");
            healthResponse.put("chainId", status.getChainId());
            healthResponse.put("blockNumber", status.getBlockNumber());
            healthResponse.put("timestamp", LocalDateTime.now());

            log.debug("Blockchain health check passed");
            return ResponseEntity.ok(healthResponse);

        } catch (Exception e) {
            log.error("Blockchain health check failed", e);

            Map<String, Object> healthResponse = new HashMap<>();
            healthResponse.put("status", "DOWN");
            healthResponse.put("error", e.getMessage());
            healthResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthResponse);
        }
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
        log.error("Unexpected error in BlockchainController", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred: " + ex.getMessage())
            .path("/api/blockchain")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}