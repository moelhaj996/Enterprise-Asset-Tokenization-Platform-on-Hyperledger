package com.enterprise.tokenization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * JPA Entity representing an asset holder (token balance tracking).
 */
@Entity
@Table(name = "asset_holders",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_asset_holder",
        columnNames = {"asset_id", "holder_address"}
    ),
    indexes = {
        @Index(name = "idx_asset_id", columnList = "asset_id"),
        @Index(name = "idx_holder_address", columnList = "holder_address"),
        @Index(name = "idx_balance", columnList = "balance")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Asset ID is required")
    @Size(max = 100, message = "Asset ID must not exceed 100 characters")
    @Column(name = "asset_id", nullable = false, length = 100)
    private String assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", referencedColumnName = "asset_id",
                insertable = false, updatable = false)
    private Asset asset;

    @NotBlank(message = "Holder address is required")
    @Size(min = 42, max = 42, message = "Holder address must be 42 characters")
    @Column(name = "holder_address", nullable = false, length = 42)
    private String holderAddress;

    @NotNull(message = "Balance is required")
    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 38, scale = 0)
    private BigInteger balance = BigInteger.ZERO;

    @NotNull(message = "First acquired date is required")
    @Column(name = "first_acquired", nullable = false, updatable = false)
    private LocalDateTime firstAcquired;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}