package com.enterprise.tokenization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {

    private Long id;

    private String assetId;

    private String assetType;

    private BigInteger totalSupply;

    private String issuer;

    private String status;

    private Map<String, Object> metadata;

    private String contractAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}