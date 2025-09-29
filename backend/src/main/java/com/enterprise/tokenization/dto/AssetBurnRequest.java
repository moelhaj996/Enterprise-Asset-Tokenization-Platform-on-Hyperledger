package com.enterprise.tokenization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetBurnRequest {

    @NotBlank(message = "Asset ID is required")
    private String assetId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigInteger amount;

    @NotBlank(message = "From address is required")
    private String from;
}