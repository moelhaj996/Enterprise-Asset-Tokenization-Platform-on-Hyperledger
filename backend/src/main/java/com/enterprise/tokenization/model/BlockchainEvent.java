package com.enterprise.tokenization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a blockchain event captured from smart contracts.
 */
@Entity
@Table(name = "blockchain_events",
    indexes = {
        @Index(name = "idx_event_name", columnList = "event_name"),
        @Index(name = "idx_contract_address", columnList = "contract_address"),
        @Index(name = "idx_block_number", columnList = "block_number"),
        @Index(name = "idx_transaction_hash", columnList = "transaction_hash"),
        @Index(name = "idx_processed", columnList = "processed"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Event name is required")
    @Size(max = 100, message = "Event name must not exceed 100 characters")
    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @NotBlank(message = "Contract address is required")
    @Size(min = 42, max = 42, message = "Contract address must be 42 characters")
    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;

    @NotNull(message = "Block number is required")
    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @NotBlank(message = "Transaction hash is required")
    @Size(min = 66, max = 66, message = "Transaction hash must be 66 characters")
    @Column(name = "transaction_hash", nullable = false, length = 66)
    private String transactionHash;

    @NotNull(message = "Log index is required")
    @Column(name = "log_index", nullable = false)
    private Integer logIndex;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @NotNull
    @Builder.Default
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}