package com.enterprise.tokenization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;

    private String transactionHash;

    private String transactionType;

    private String assetId;

    private String fromAddress;

    private String toAddress;

    private BigInteger amount;

    private String status;

    private Long blockNumber;

    private String blockHash;

    private BigInteger gasUsed;

    private String errorMessage;

    private LocalDateTime timestamp;

    private LocalDateTime createdAt;
}