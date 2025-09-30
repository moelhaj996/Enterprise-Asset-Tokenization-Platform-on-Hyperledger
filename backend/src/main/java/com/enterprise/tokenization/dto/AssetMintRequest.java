package com.enterprise.tokenization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigInteger;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMintRequest {

    @NotBlank(message = "Asset ID is required")
    private String assetId;

    @NotBlank(message = "Asset type is required")
    private String assetType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigInteger amount;

    @NotBlank(message = "Recipient address is required")
    private String recipient;

    @NotBlank(message = "Issuer address is required")
    private String issuer;

    private Map<String, Object> metadata;
}