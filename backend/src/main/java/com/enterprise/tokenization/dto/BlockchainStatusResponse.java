package com.enterprise.tokenization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainStatusResponse {

    private String chainId;

    private BigInteger blockNumber;

    private Integer peerCount;

    private Boolean syncing;
}