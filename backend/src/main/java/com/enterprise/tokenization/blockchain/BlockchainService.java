package com.enterprise.tokenization.blockchain;

import com.enterprise.tokenization.dto.BlockchainStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for interacting with the AssetToken smart contract on Hyperledger Besu.
 * Provides methods for minting, burning, querying assets, and monitoring blockchain status.
 *
 * <p>This service acts as a bridge between the Spring Boot application and the blockchain,
 * handling transaction creation, signing, and submission, as well as querying contract state.</p>
 *
 * @author Enterprise Tokenization Platform
 * @version 1.0
 * @since 2025-09-29
 */
@Slf4j
@Service
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final TransactionManager transactionManager;
    private final ContractGasProvider contractGasProvider;
    private final ObjectMapper objectMapper;

    @Value("${blockchain.contract.asset-token-address}")
    private String contractAddress;

    /**
     * Constructs the BlockchainService with required Web3j dependencies.
     *
     * @param web3j                Web3j instance for blockchain connectivity
     * @param credentials          Credentials for signing transactions
     * @param transactionManager   Transaction manager for handling blockchain transactions
     * @param contractGasProvider  Gas provider for contract operations
     */
    @Autowired
    public BlockchainService(
            Web3j web3j,
            Credentials credentials,
            TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.transactionManager = transactionManager;
        this.contractGasProvider = contractGasProvider;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Mints new asset tokens on the blockchain.
     *
     * <p>Creates a new asset with the specified metadata and mints tokens to the recipient address.
     * This operation requires MINTER_ROLE on the smart contract.</p>
     *
     * @param assetId   Unique identifier for the asset (e.g., "BOND-001")
     * @param assetType Type of asset (e.g., "CORPORATE_BOND", "INVOICE", "SUPPLY_CHAIN")
     * @param amount    Amount of tokens to mint (in smallest unit)
     * @param recipient Ethereum address to receive the minted tokens
     * @param issuer    Name or identifier of the asset issuer
     * @return Transaction hash of the minting transaction
     * @throws Exception if the transaction fails or cannot be processed
     */
    public String mintAsset(String assetId, String assetType, BigInteger amount, String recipient, String issuer) throws Exception {
        log.info("Minting asset: assetId={}, assetType={}, amount={}, recipient={}, issuer={}",
                assetId, assetType, amount, recipient, issuer);

        try {
            // Validate inputs
            validateMintParameters(assetId, assetType, amount, recipient, issuer);

            // Create function call: mint(address to, uint256 amount, string assetId, string assetType, string issuer)
            Function function = new Function(
                    "mint",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(recipient),
                            new Uint256(amount),
                            new Utf8String(assetId),
                            new Utf8String(assetType),
                            new Utf8String(issuer)
                    ),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // Send transaction
            String txHash = transactionManager.sendTransaction(
                    contractGasProvider.getGasPrice(),
                    contractGasProvider.getGasLimit(),
                    contractAddress,
                    encodedFunction,
                    BigInteger.ZERO
            ).getTransactionHash();

            log.info("Asset minted successfully. Transaction hash: {}", txHash);

            // Wait for transaction receipt to ensure it's mined
            TransactionReceipt receipt = waitForTransactionReceipt(txHash);

            if (receipt.isStatusOK()) {
                log.info("Transaction confirmed in block {}", receipt.getBlockNumber());
                return txHash;
            } else {
                throw new Exception("Transaction failed with status: " + receipt.getStatus());
            }

        } catch (Exception e) {
            log.error("Failed to mint asset: {}", e.getMessage(), e);
            throw new Exception("Failed to mint asset [" + assetId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Burns asset tokens from a specific address.
     *
     * <p>Destroys tokens associated with an asset, reducing the total supply.
     * This operation requires BURNER_ROLE on the smart contract.</p>
     *
     * @param assetId Unique identifier for the asset
     * @param amount  Amount of tokens to burn (in smallest unit)
     * @param from    Ethereum address to burn tokens from
     * @return Transaction hash of the burn transaction
     * @throws Exception if the transaction fails or cannot be processed
     */
    public String burnAsset(String assetId, BigInteger amount, String from) throws Exception {
        log.info("Burning asset: assetId={}, amount={}, from={}", assetId, amount, from);

        try {
            // Validate inputs
            validateBurnParameters(assetId, amount, from);

            // Create function call: burn(address from, uint256 amount, string assetId)
            Function function = new Function(
                    "burn",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(from),
                            new Uint256(amount),
                            new Utf8String(assetId)
                    ),
                    Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // Send transaction
            String txHash = transactionManager.sendTransaction(
                    contractGasProvider.getGasPrice(),
                    contractGasProvider.getGasLimit(),
                    contractAddress,
                    encodedFunction,
                    BigInteger.ZERO
            ).getTransactionHash();

            log.info("Asset burned successfully. Transaction hash: {}", txHash);

            // Wait for transaction receipt
            TransactionReceipt receipt = waitForTransactionReceipt(txHash);

            if (receipt.isStatusOK()) {
                log.info("Burn transaction confirmed in block {}", receipt.getBlockNumber());
                return txHash;
            } else {
                throw new Exception("Burn transaction failed with status: " + receipt.getStatus());
            }

        } catch (Exception e) {
            log.error("Failed to burn asset: {}", e.getMessage(), e);
            throw new Exception("Failed to burn asset [" + assetId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the token balance of a specific address.
     *
     * <p>Queries the ERC20 balanceOf function to get the token balance.</p>
     *
     * @param address Ethereum address to query
     * @return Token balance (in smallest unit)
     * @throws Exception if the query fails
     */
    public BigInteger getBalance(String address) throws Exception {
        log.debug("Getting balance for address: {}", address);

        try {
            // Validate address
            if (address == null || address.trim().isEmpty()) {
                throw new IllegalArgumentException("Address cannot be null or empty");
            }

            // Create function call: balanceOf(address)
            Function function = new Function(
                    "balanceOf",
                    Collections.singletonList(new org.web3j.abi.datatypes.Address(address)),
                    Collections.singletonList(new TypeReference<Uint256>() {})
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // Call contract
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new Exception("Error calling balanceOf: " + response.getError().getMessage());
            }

            List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

            if (result.isEmpty()) {
                throw new Exception("No result returned from balanceOf call");
            }

            BigInteger balance = (BigInteger) result.get(0).getValue();
            log.debug("Balance for address {}: {}", address, balance);

            return balance;

        } catch (Exception e) {
            log.error("Failed to get balance for address {}: {}", address, e.getMessage(), e);
            throw new Exception("Failed to get balance for address [" + address + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the transaction receipt for a given transaction hash.
     *
     * <p>Provides detailed information about a transaction including its status,
     * block number, gas used, and event logs.</p>
     *
     * @param txHash Transaction hash to query
     * @return TransactionReceipt object containing transaction details
     * @throws Exception if the receipt cannot be retrieved
     */
    public TransactionReceipt getTransactionReceipt(String txHash) throws Exception {
        log.debug("Getting transaction receipt for hash: {}", txHash);

        try {
            if (txHash == null || txHash.trim().isEmpty()) {
                throw new IllegalArgumentException("Transaction hash cannot be null or empty");
            }

            EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txHash).send();

            if (response.hasError()) {
                throw new Exception("Error getting transaction receipt: " + response.getError().getMessage());
            }

            Optional<TransactionReceipt> receiptOpt = response.getTransactionReceipt();

            if (!receiptOpt.isPresent()) {
                throw new Exception("Transaction receipt not found for hash: " + txHash);
            }

            TransactionReceipt receipt = receiptOpt.get();
            log.debug("Transaction receipt retrieved: block={}, status={}",
                    receipt.getBlockNumber(), receipt.getStatus());

            return receipt;

        } catch (Exception e) {
            log.error("Failed to get transaction receipt for hash {}: {}", txHash, e.getMessage(), e);
            throw new Exception("Failed to get transaction receipt for hash [" + txHash + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves metadata for a specific asset from the blockchain.
     *
     * <p>Returns asset details including asset ID, type, issuance date, and issuer
     * as a JSON string.</p>
     *
     * @param assetId Unique identifier for the asset
     * @return JSON string containing asset metadata
     * @throws Exception if the asset does not exist or query fails
     */
    public String getAssetMetadata(String assetId) throws Exception {
        log.debug("Getting metadata for asset: {}", assetId);

        try {
            if (assetId == null || assetId.trim().isEmpty()) {
                throw new IllegalArgumentException("Asset ID cannot be null or empty");
            }

            // Create function call: getAssetMetadata(string assetId)
            Function function = new Function(
                    "getAssetMetadata",
                    Collections.singletonList(new Utf8String(assetId)),
                    Arrays.asList(
                            new TypeReference<Utf8String>() {},  // assetId
                            new TypeReference<Utf8String>() {},  // assetType
                            new TypeReference<Uint256>() {},     // issuanceDate
                            new TypeReference<Utf8String>() {},  // issuer
                            new TypeReference<Bool>() {}         // exists
                    )
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // Call contract
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new Exception("Error calling getAssetMetadata: " + response.getError().getMessage());
            }

            List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

            if (result.isEmpty() || result.size() < 5) {
                throw new Exception("Invalid result returned from getAssetMetadata");
            }

            // Check if asset exists
            boolean exists = (Boolean) result.get(4).getValue();
            if (!exists) {
                throw new Exception("Asset does not exist: " + assetId);
            }

            // Build metadata JSON
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("assetId", result.get(0).getValue());
            metadata.put("assetType", result.get(1).getValue());
            metadata.put("issuanceDate", result.get(2).getValue());
            metadata.put("issuer", result.get(3).getValue());
            metadata.put("exists", exists);

            String metadataJson = objectMapper.writeValueAsString(metadata);
            log.debug("Asset metadata retrieved: {}", metadataJson);

            return metadataJson;

        } catch (Exception e) {
            log.error("Failed to get asset metadata for {}: {}", assetId, e.getMessage(), e);
            throw new Exception("Failed to get asset metadata for [" + assetId + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all asset IDs registered on the blockchain.
     *
     * <p>Returns a complete list of all assets that have been minted through the contract.</p>
     *
     * @return List of asset IDs
     * @throws Exception if the query fails
     */
    public List<String> getAllAssetIds() throws Exception {
        log.debug("Getting all asset IDs from contract");

        try {
            // Create function call: getAllAssetIds()
            Function function = new Function(
                    "getAllAssetIds",
                    Collections.emptyList(),
                    Collections.singletonList(new TypeReference<DynamicArray<Utf8String>>() {})
            );

            String encodedFunction = FunctionEncoder.encode(function);

            // Call contract
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new Exception("Error calling getAllAssetIds: " + response.getError().getMessage());
            }

            List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

            if (result.isEmpty()) {
                log.debug("No assets found in contract");
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Utf8String> assetIdList = (List<Utf8String>) result.get(0).getValue();

            List<String> assetIds = assetIdList.stream()
                    .map(Utf8String::getValue)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} asset IDs from contract", assetIds.size());

            return assetIds;

        } catch (Exception e) {
            log.error("Failed to get all asset IDs: {}", e.getMessage(), e);
            throw new Exception("Failed to get all asset IDs: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the current network status and health metrics.
     *
     * <p>Provides information about the blockchain network including chain ID,
     * latest block number, peer count, and sync status. Useful for monitoring
     * and health checks.</p>
     *
     * @return BlockchainStatusResponse containing network status information
     * @throws Exception if the network status cannot be retrieved
     */
    public BlockchainStatusResponse getNetworkStatus() throws Exception {
        log.debug("Getting blockchain network status");

        try {
            // Get chain ID
            String chainId = web3j.ethChainId().send().getChainId().toString();

            // Get latest block number
            EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
            BigInteger blockNumber = blockNumberResponse.getBlockNumber();

            // Get peer count (note: may not be available on all networks)
            Integer peerCount;
            try {
                peerCount = web3j.netPeerCount().send().getQuantity().intValue();
            } catch (Exception e) {
                log.warn("Failed to get peer count: {}", e.getMessage());
                peerCount = 0;
            }

            // Get sync status
            Boolean syncing = web3j.ethSyncing().send().isSyncing();

            BlockchainStatusResponse status = BlockchainStatusResponse.builder()
                    .chainId(chainId)
                    .blockNumber(blockNumber)
                    .peerCount(peerCount)
                    .syncing(syncing)
                    .build();

            log.info("Network status: chainId={}, blockNumber={}, peerCount={}, syncing={}",
                    chainId, blockNumber, peerCount, syncing);

            return status;

        } catch (Exception e) {
            log.error("Failed to get network status: {}", e.getMessage(), e);
            throw new Exception("Failed to get blockchain network status: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for a transaction receipt with polling and timeout.
     *
     * <p>Polls the blockchain for the transaction receipt until it's available or timeout occurs.
     * Default timeout is 60 seconds with 2-second polling intervals.</p>
     *
     * @param txHash Transaction hash to wait for
     * @return TransactionReceipt once the transaction is mined
     * @throws Exception if timeout occurs or transaction fails
     */
    private TransactionReceipt waitForTransactionReceipt(String txHash) throws Exception {
        int attempts = 30; // 30 attempts * 2 seconds = 60 seconds timeout
        int sleepDuration = 2000; // 2 seconds

        log.debug("Waiting for transaction receipt: {}", txHash);

        for (int i = 0; i < attempts; i++) {
            try {
                EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txHash).send();

                if (response.getTransactionReceipt().isPresent()) {
                    TransactionReceipt receipt = response.getTransactionReceipt().get();
                    log.debug("Transaction receipt received after {} attempts", i + 1);
                    return receipt;
                }

                Thread.sleep(sleepDuration);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Transaction receipt polling interrupted", e);
            }
        }

        throw new Exception("Transaction receipt not received after " + (attempts * sleepDuration / 1000) + " seconds");
    }

    /**
     * Validates mint operation parameters.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private void validateMintParameters(String assetId, String assetType, BigInteger amount, String recipient, String issuer) {
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID cannot be null or empty");
        }
        if (assetType == null || assetType.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset type cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient address cannot be null or empty");
        }
        if (issuer == null || issuer.trim().isEmpty()) {
            throw new IllegalArgumentException("Issuer cannot be null or empty");
        }
    }

    /**
     * Validates burn operation parameters.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private void validateBurnParameters(String assetId, BigInteger amount, String from) {
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("From address cannot be null or empty");
        }
    }
}