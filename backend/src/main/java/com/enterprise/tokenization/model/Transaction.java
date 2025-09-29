package com.enterprise.tokenization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a blockchain transaction in the platform.
 */
@Entity
@Table(name = "transactions",
    uniqueConstraints = @UniqueConstraint(columnNames = "transaction_hash"),
    indexes = {
        @Index(name = "idx_asset_id", columnList = "asset_id"),
        @Index(name = "idx_from_address", columnList = "from_address"),
        @Index(name = "idx_to_address", columnList = "to_address"),
        @Index(name = "idx_block_number", columnList = "block_number"),
        @Index(name = "idx_status", columnList = "status")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction hash is required")
    @Size(min = 66, max = 66, message = "Transaction hash must be 66 characters")
    @Column(name = "transaction_hash", nullable = false, unique = true, length = 66)
    private String transactionHash;

    @NotBlank(message = "Asset ID is required")
    @Column(name = "asset_id", nullable = false, length = 100)
    private String assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", referencedColumnName = "asset_id",
                insertable = false, updatable = false)
    private Asset asset;

    @NotBlank(message = "From address is required")
    @Size(max = 42, message = "From address must not exceed 42 characters")
    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @NotBlank(message = "To address is required")
    @Size(max = 42, message = "To address must not exceed 42 characters")
    @Column(name = "to_address", nullable = false, length = 42)
    private String toAddress;

    @NotNull(message = "Amount is required")
    @Column(name = "amount", nullable = false, precision = 38, scale = 0)
    private BigInteger amount;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @NotNull(message = "Block number is required")
    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @NotNull(message = "Block timestamp is required")
    @Column(name = "block_timestamp", nullable = false)
    private LocalDateTime blockTimestamp;

    @Column(name = "gas_used")
    private Long gasUsed;

    @Column(name = "gas_price")
    private Long gasPrice;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}