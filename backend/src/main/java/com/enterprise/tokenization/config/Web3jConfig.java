package com.enterprise.tokenization.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import okhttp3.OkHttpClient;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for Web3j blockchain connectivity.
 * Provides beans for interacting with Hyperledger Besu network.
 */
@Slf4j
@Configuration
public class Web3jConfig {

    private final BlockchainProperties blockchainProperties;

    public Web3jConfig(BlockchainProperties blockchainProperties) {
        this.blockchainProperties = blockchainProperties;
    }

    /**
     * Creates Web3j instance with connection retry logic.
     * Configures HTTP client with custom timeouts and retry mechanism.
     *
     * @return Web3j instance connected to Besu node
     */
    @Bean
    public Web3j web3j() {
        log.info("Initializing Web3j connection to Besu at: {}", blockchainProperties.getRpcUrl());

        // Configure OkHttpClient with retry logic and timeouts
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        OkHttpClient httpClient = clientBuilder.build();
        HttpService httpService = new HttpService(blockchainProperties.getRpcUrl(), httpClient);

        Web3j web3j = Web3j.build(httpService);

        try {
            // Test connection
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("Successfully connected to Besu node. Client version: {}", clientVersion);
        } catch (Exception e) {
            log.error("Failed to connect to Besu node at {}: {}",
                    blockchainProperties.getRpcUrl(), e.getMessage());
            log.warn("Web3j bean created but connection test failed. Ensure Besu node is running.");
        }

        return web3j;
    }

    /**
     * Loads blockchain credentials from environment variable.
     * Private key must be provided via DEPLOYER_PRIVATE_KEY environment variable.
     *
     * @return Credentials object for signing transactions
     * @throws IllegalStateException if private key is not configured
     */
    @Bean
    public Credentials credentials() {
        String privateKey = blockchainProperties.getPrivateKey();

        if (privateKey == null || privateKey.trim().isEmpty()) {
            log.error("Private key not configured. Set DEPLOYER_PRIVATE_KEY environment variable.");
            throw new IllegalStateException(
                    "Blockchain private key not configured. Please set DEPLOYER_PRIVATE_KEY environment variable."
            );
        }

        // Remove 0x prefix if present
        if (privateKey.startsWith("0x") || privateKey.startsWith("0X")) {
            privateKey = privateKey.substring(2);
        }

        try {
            Credentials credentials = Credentials.create(privateKey);
            log.info("Blockchain credentials loaded successfully. Address: {}", credentials.getAddress());
            return credentials;
        } catch (Exception e) {
            log.error("Failed to create credentials from private key: {}", e.getMessage());
            throw new IllegalStateException("Invalid private key format", e);
        }
    }

    /**
     * Creates TransactionManager for handling blockchain transactions.
     * Uses RawTransactionManager for direct transaction signing and submission.
     *
     * @param web3j Web3j instance
     * @param credentials Blockchain credentials
     * @return TransactionManager instance
     */
    @Bean
    public TransactionManager transactionManager(Web3j web3j, Credentials credentials) {
        long chainId = blockchainProperties.getChainId();
        log.info("Creating TransactionManager with chain ID: {}", chainId);

        return new RawTransactionManager(
                web3j,
                credentials,
                chainId
        );
    }

    /**
     * Provides gas provider for smart contract transactions.
     * Uses custom gas settings from configuration or defaults to DefaultGasProvider.
     *
     * @return ContractGasProvider instance
     */
    @Bean
    public ContractGasProvider contractGasProvider() {
        BigInteger gasPrice = blockchainProperties.getGasPrice();
        BigInteger gasLimit = blockchainProperties.getGasLimit();

        log.info("Configuring gas provider - Price: {}, Limit: {}", gasPrice, gasLimit);

        // If custom gas settings are provided, use them
        if (gasPrice != null && gasLimit != null) {
            return new CustomGasProvider(gasPrice, gasLimit);
        }

        // Otherwise use default gas provider
        log.info("Using DefaultGasProvider");
        return new DefaultGasProvider();
    }

    /**
     * Custom gas provider implementation with configurable gas price and limit.
     */
    private static class CustomGasProvider implements ContractGasProvider {
        private final BigInteger gasPrice;
        private final BigInteger gasLimit;

        public CustomGasProvider(BigInteger gasPrice, BigInteger gasLimit) {
            this.gasPrice = gasPrice;
            this.gasLimit = gasLimit;
        }

        @Override
        public BigInteger getGasPrice(String contractFunc) {
            return gasPrice;
        }

        @Override
        public BigInteger getGasPrice() {
            return gasPrice;
        }

        @Override
        public BigInteger getGasLimit(String contractFunc) {
            return gasLimit;
        }

        @Override
        public BigInteger getGasLimit() {
            return gasLimit;
        }
    }

    /**
     * Configuration properties for blockchain connectivity.
     * Maps to blockchain.besu.* properties in application.yml
     */
    @Data
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "blockchain.besu")
    public static class BlockchainProperties {

        @NotBlank(message = "Besu RPC URL must be configured")
        private String rpcUrl;

        private String wsUrl;

        @NotNull(message = "Chain ID must be configured")
        private Long chainId;

        private BigInteger gasPrice;

        private BigInteger gasLimit;

        private String privateKey;
    }
}