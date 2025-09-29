package com.enterprise.tokenization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a tokenized asset in the platform.
 */
@Entity
@Table(name = "assets", uniqueConstraints = {
    @UniqueConstraint(columnNames = "asset_id"),
    @UniqueConstraint(columnNames = "contract_address")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Asset ID is required")
    @Size(max = 100, message = "Asset ID must not exceed 100 characters")
    @Column(name = "asset_id", nullable = false, unique = true, length = 100)
    private String assetId;

    @NotBlank(message = "Asset type is required")
    @Size(max = 50, message = "Asset type must not exceed 50 characters")
    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    @NotNull(message = "Total supply is required")
    @Column(name = "total_supply", nullable = false, precision = 38, scale = 0)
    private BigInteger totalSupply;

    @NotBlank(message = "Contract address is required")
    @Size(min = 42, max = 42, message = "Contract address must be 42 characters")
    @Column(name = "contract_address", nullable = false, unique = true, length = 42)
    private String contractAddress;

    @NotBlank(message = "Issuer is required")
    @Size(max = 100, message = "Issuer must not exceed 100 characters")
    @Column(name = "issuer", nullable = false, length = 100)
    private String issuer;

    @NotNull(message = "Issuance date is required")
    @Column(name = "issuance_date", nullable = false)
    private LocalDateTime issuanceDate;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}