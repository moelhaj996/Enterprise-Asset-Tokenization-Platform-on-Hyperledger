package com.enterprise.tokenization.service;

import com.enterprise.tokenization.dto.*;
import com.enterprise.tokenization.model.*;
import com.enterprise.tokenization.repository.AssetHolderRepository;
import com.enterprise.tokenization.repository.AssetRepository;
import com.enterprise.tokenization.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing asset operations.
 * Handles business logic for asset minting, burning, and querying.
 * Coordinates between blockchain operations and database persistence.
 */
@Service
@Slf4j
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AssetHolderRepository assetHolderRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mints a new asset or additional tokens for an existing asset.
     * Creates blockchain transaction and persists data to database.
     *
     * @param request the asset minting request containing asset details
     * @return AssetResponse with the minted asset details
     * @throws Exception if minting fails or validation errors occur
     */
    @Transactional
    public AssetResponse mintAsset(AssetMintRequest request) throws Exception {
        log.info("Minting asset: {} of type: {} with amount: {}",
                request.getAssetId(), request.getAssetType(), request.getAmount());

        // Validate request
        validateMintRequest(request);

        try {
            // Call blockchain service to mint the asset
            String transactionHash = blockchainService.mintAsset(
                request.getAssetId(),
                request.getAssetType(),
                request.getAmount(),
                request.getRecipient(),
                request.getIssuer()
            );

            log.info("Asset minted on blockchain. Transaction hash: {}", transactionHash);

            // Check if asset already exists or create new one
            Asset asset = assetRepository.findByAssetId(request.getAssetId())
                    .orElse(null);

            if (asset == null) {
                // Create new asset entity
                asset = Asset.builder()
                        .assetId(request.getAssetId())
                        .assetType(request.getAssetType())
                        .totalSupply(request.getAmount())
                        .issuer(request.getIssuer())
                        .contractAddress(generateContractAddress(request.getAssetId()))
                        .issuanceDate(LocalDateTime.now())
                        .metadata(serializeMetadata(request.getMetadata()))
                        .build();
            } else {
                // Update existing asset's total supply
                asset.setTotalSupply(asset.getTotalSupply().add(request.getAmount()));
                asset.setMetadata(serializeMetadata(request.getMetadata()));
            }

            asset = assetRepository.save(asset);
            log.info("Asset saved to database with ID: {}", asset.getId());

            // Create transaction record with SUCCESS status
            Transaction transaction = Transaction.builder()
                    .transactionHash(transactionHash)
                    .assetId(request.getAssetId())
                    .fromAddress(request.getIssuer())
                    .toAddress(request.getRecipient())
                    .amount(request.getAmount())
                    .transactionType(TransactionType.MINT)
                    .blockNumber(0L) // Will be updated when confirmed
                    .blockTimestamp(LocalDateTime.now())
                    .status(TransactionStatus.SUCCESS)
                    .build();

            transactionRepository.save(transaction);
            log.info("Transaction record created with hash: {}", transactionHash);

            // Create or update AssetHolder record
            updateAssetHolder(request.getAssetId(), request.getRecipient(), request.getAmount(), true);

            return mapToAssetResponse(asset);

        } catch (Exception e) {
            log.error("Failed to mint asset: {}", request.getAssetId(), e);

            // Create failed transaction record
            Transaction failedTransaction = Transaction.builder()
                    .transactionHash("FAILED_" + System.currentTimeMillis())
                    .assetId(request.getAssetId())
                    .fromAddress(request.getIssuer())
                    .toAddress(request.getRecipient())
                    .amount(request.getAmount())
                    .transactionType(TransactionType.MINT)
                    .blockNumber(0L)
                    .blockTimestamp(LocalDateTime.now())
                    .status(TransactionStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();

            transactionRepository.save(failedTransaction);

            throw new Exception("Failed to mint asset: " + e.getMessage(), e);
        }
    }

    /**
     * Burns (destroys) tokens of an existing asset.
     * Reduces the total supply and holder balance.
     *
     * @param request the asset burn request containing asset ID and amount
     * @return TransactionResponse with the burn transaction details
     * @throws Exception if burning fails or asset not found
     */
    @Transactional
    public TransactionResponse burnAsset(AssetBurnRequest request) throws Exception {
        log.info("Burning asset: {} with amount: {} from address: {}",
                request.getAssetId(), request.getAmount(), request.getFrom());

        // Validate asset exists
        Asset asset = assetRepository.findByAssetId(request.getAssetId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asset not found with ID: " + request.getAssetId()));

        // Validate holder has sufficient balance
        AssetHolder holder = assetHolderRepository.findByAssetIdAndHolderAddress(
                request.getAssetId(), request.getFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No asset holdings found for address: " + request.getFrom()));

        if (holder.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Available: " + holder.getBalance() +
                    ", Required: " + request.getAmount());
        }

        try {
            // Call blockchain service to burn the asset
            String transactionHash = blockchainService.burnAsset(
                request.getAssetId(),
                request.getAmount(),
                request.getFrom()
            );

            log.info("Asset burned on blockchain. Transaction hash: {}", transactionHash);

            // Update asset's total supply
            asset.setTotalSupply(asset.getTotalSupply().subtract(request.getAmount()));
            assetRepository.save(asset);

            // Create transaction entity
            Transaction transaction = Transaction.builder()
                    .transactionHash(transactionHash)
                    .assetId(request.getAssetId())
                    .fromAddress(request.getFrom())
                    .toAddress("0x0000000000000000000000000000000000000000") // Burn address
                    .amount(request.getAmount())
                    .transactionType(TransactionType.BURN)
                    .blockNumber(0L)
                    .blockTimestamp(LocalDateTime.now())
                    .status(TransactionStatus.SUCCESS)
                    .build();

            transaction = transactionRepository.save(transaction);
            log.info("Burn transaction record created with hash: {}", transactionHash);

            // Update AssetHolder balance (subtract)
            updateAssetHolder(request.getAssetId(), request.getFrom(), request.getAmount(), false);

            return mapToTransactionResponse(transaction);

        } catch (Exception e) {
            log.error("Failed to burn asset: {}", request.getAssetId(), e);

            // Create failed transaction record
            Transaction failedTransaction = Transaction.builder()
                    .transactionHash("FAILED_" + System.currentTimeMillis())
                    .assetId(request.getAssetId())
                    .fromAddress(request.getFrom())
                    .toAddress("0x0000000000000000000000000000000000000000")
                    .amount(request.getAmount())
                    .transactionType(TransactionType.BURN)
                    .blockNumber(0L)
                    .blockTimestamp(LocalDateTime.now())
                    .status(TransactionStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();

            transactionRepository.save(failedTransaction);

            throw new Exception("Failed to burn asset: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a single asset by its asset ID.
     *
     * @param assetId the unique asset identifier
     * @return AssetResponse with asset details
     * @throws IllegalArgumentException if asset not found
     */
    public AssetResponse getAsset(String assetId) {
        log.info("Retrieving asset: {}", assetId);

        Asset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asset not found with ID: " + assetId));

        return mapToAssetResponse(asset);
    }

    /**
     * Retrieves all assets in the system.
     *
     * @return List of AssetResponse objects
     */
    public List<AssetResponse> getAllAssets() {
        log.info("Retrieving all assets");

        List<Asset> assets = assetRepository.findAll();
        log.info("Found {} assets", assets.size());

        return assets.stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all assets of a specific type.
     *
     * @param type the asset type to filter by
     * @return List of AssetResponse objects matching the type
     */
    public List<AssetResponse> getAssetsByType(String type) {
        log.info("Retrieving assets by type: {}", type);

        List<Asset> assets = assetRepository.findByAssetType(type);
        log.info("Found {} assets of type: {}", assets.size(), type);

        return assets.stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all transactions associated with a specific asset.
     *
     * @param assetId the asset ID to filter transactions
     * @return List of TransactionResponse objects
     */
    public List<TransactionResponse> getAssetTransactions(String assetId) {
        log.info("Retrieving transactions for asset: {}", assetId);

        // Validate asset exists
        if (!assetRepository.existsByAssetId(assetId)) {
            throw new IllegalArgumentException("Asset not found with ID: " + assetId);
        }

        List<Transaction> transactions = transactionRepository.findByAssetId(assetId);
        log.info("Found {} transactions for asset: {}", transactions.size(), assetId);

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the balance of an asset for a specific holder address.
     * Attempts to get balance from blockchain first, falls back to database.
     *
     * @param assetId the asset ID
     * @param address the holder address
     * @return BigInteger representing the balance
     * @throws Exception if retrieval fails
     */
    public BigInteger getAssetBalance(String assetId, String address) throws Exception {
        log.info("Retrieving balance for asset: {} and address: {}", assetId, address);

        try {
            // Try to get balance from blockchain
            BigInteger balance = blockchainService.getBalance(assetId, address);
            log.info("Balance from blockchain: {}", balance);
            return balance;
        } catch (Exception e) {
            log.warn("Failed to get balance from blockchain, falling back to database", e);

            // Fall back to database
            AssetHolder holder = assetHolderRepository.findByAssetIdAndHolderAddress(assetId, address)
                    .orElse(null);

            if (holder == null) {
                log.info("No holdings found, returning zero balance");
                return BigInteger.ZERO;
            }

            log.info("Balance from database: {}", holder.getBalance());
            return holder.getBalance();
        }
    }

    /**
     * Retrieves all assets held by a specific address.
     *
     * @param address the holder address
     * @return List of AssetResponse objects held by the address
     */
    public List<AssetResponse> getHolderAssets(String address) {
        log.info("Retrieving assets for holder: {}", address);

        List<AssetHolder> holders = assetHolderRepository.findByHolderAddress(address);
        log.info("Found {} asset holdings for address: {}", holders.size(), address);

        return holders.stream()
                .filter(holder -> holder.getBalance().compareTo(BigInteger.ZERO) > 0)
                .map(holder -> {
                    Asset asset = assetRepository.findByAssetId(holder.getAssetId())
                            .orElse(null);
                    return asset != null ? mapToAssetResponse(asset) : null;
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validates the mint request parameters.
     *
     * @param request the mint request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateMintRequest(AssetMintRequest request) {
        if (request.getAmount().compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (request.getAssetId() == null || request.getAssetId().trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID is required");
        }

        if (request.getRecipient() == null || request.getRecipient().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient address is required");
        }

        if (request.getIssuer() == null || request.getIssuer().trim().isEmpty()) {
            throw new IllegalArgumentException("Issuer address is required");
        }
    }

    /**
     * Creates or updates an asset holder record.
     *
     * @param assetId the asset ID
     * @param holderAddress the holder address
     * @param amount the amount to add or subtract
     * @param isAddition true to add, false to subtract
     */
    private void updateAssetHolder(String assetId, String holderAddress,
                                   BigInteger amount, boolean isAddition) {
        AssetHolder holder = assetHolderRepository
                .findByAssetIdAndHolderAddress(assetId, holderAddress)
                .orElse(null);

        if (holder == null) {
            // Create new holder record
            holder = AssetHolder.builder()
                    .assetId(assetId)
                    .holderAddress(holderAddress)
                    .balance(isAddition ? amount : BigInteger.ZERO)
                    .firstAcquired(LocalDateTime.now())
                    .build();
            log.info("Created new asset holder record for address: {}", holderAddress);
        } else {
            // Update existing holder's balance
            BigInteger newBalance = isAddition
                    ? holder.getBalance().add(amount)
                    : holder.getBalance().subtract(amount);
            holder.setBalance(newBalance);
            log.info("Updated asset holder balance for address: {} to: {}",
                    holderAddress, newBalance);
        }

        assetHolderRepository.save(holder);
    }

    /**
     * Generates a contract address for an asset.
     * In a real implementation, this would come from the blockchain.
     *
     * @param assetId the asset ID
     * @return generated contract address
     */
    private String generateContractAddress(String assetId) {
        // Placeholder implementation
        // In reality, this should come from the blockchain deployment
        return "0x" + String.format("%040x", assetId.hashCode()).substring(0, 40);
    }

    /**
     * Serializes metadata map to JSON string.
     *
     * @param metadata the metadata map
     * @return JSON string representation
     */
    private String serializeMetadata(java.util.Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata", e);
            return null;
        }
    }

    /**
     * Deserializes JSON string to metadata map.
     *
     * @param metadataJson the JSON string
     * @return metadata map
     */
    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> deserializeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(metadataJson, java.util.Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize metadata", e);
            return null;
        }
    }

    /**
     * Maps Asset entity to AssetResponse DTO.
     *
     * @param asset the asset entity
     * @return AssetResponse DTO
     */
    private AssetResponse mapToAssetResponse(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getId())
                .assetId(asset.getAssetId())
                .assetType(asset.getAssetType())
                .totalSupply(asset.getTotalSupply())
                .issuer(asset.getIssuer())
                .status("ACTIVE") // Could be enhanced with actual status field
                .metadata(deserializeMetadata(asset.getMetadata()))
                .contractAddress(asset.getContractAddress())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }

    /**
     * Maps Transaction entity to TransactionResponse DTO.
     *
     * @param transaction the transaction entity
     * @return TransactionResponse DTO
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionHash(transaction.getTransactionHash())
                .transactionType(transaction.getTransactionType().name())
                .assetId(transaction.getAssetId())
                .fromAddress(transaction.getFromAddress())
                .toAddress(transaction.getToAddress())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .blockNumber(transaction.getBlockNumber())
                .gasUsed(transaction.getGasUsed() != null ? BigInteger.valueOf(transaction.getGasUsed()) : null)
                .errorMessage(transaction.getErrorMessage())
                .timestamp(transaction.getBlockTimestamp())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}