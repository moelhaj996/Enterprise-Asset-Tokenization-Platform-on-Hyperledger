package com.enterprise.tokenization.service;

import com.enterprise.tokenization.model.*;
import com.enterprise.tokenization.repository.AssetHolderRepository;
import com.enterprise.tokenization.repository.AssetRepository;
import com.enterprise.tokenization.repository.BlockchainEventRepository;
import com.enterprise.tokenization.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service class for listening to and processing blockchain events.
 * Subscribes to smart contract events and synchronizes blockchain state with the database.
 *
 * Features:
 * - Real-time event listening for AssetMinted, AssetBurned, and Transfer events
 * - Historical event replay for blockchain synchronization
 * - Automatic reconnection on connection drops
 * - Comprehensive error handling and logging
 *
 * @author Enterprise Tokenization Platform
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventListenerService {

    @Autowired
    private final Web3j web3j;

    @Autowired
    private final AssetRepository assetRepository;

    @Autowired
    private final TransactionRepository transactionRepository;

    @Autowired
    private final BlockchainEventRepository blockchainEventRepository;

    @Autowired
    private final AssetHolderRepository assetHolderRepository;

    @Value("${blockchain.contract.asset-token-address:}")
    private String contractAddress;

    @Value("${blockchain.event.replay-from-block:0}")
    private long replayFromBlock;

    @Value("${blockchain.event.reconnect-delay-seconds:30}")
    private long reconnectDelaySeconds;

    private final Map<String, Disposable> eventSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean isListening = false;

    // Event signatures
    private static final String ASSET_MINTED_EVENT = "AssetMinted";
    private static final String ASSET_BURNED_EVENT = "AssetBurned";
    private static final String TRANSFER_EVENT = "Transfer";

    /**
     * Starts event listeners when the application is ready.
     * Automatically triggered by Spring on ApplicationReadyEvent.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        try {
            log.info("Application ready - starting blockchain event listeners");

            if (contractAddress == null || contractAddress.trim().isEmpty()) {
                log.warn("Contract address not configured - skipping event listener initialization");
                return;
            }

            // Replay past events for synchronization
            replayPastEvents();

            // Start real-time event listeners
            listenToMintEvents();
            listenToBurnEvents();
            listenToTransferEvents();

            isListening = true;
            log.info("Blockchain event listeners started successfully for contract: {}", contractAddress);

            // Schedule periodic health check
            scheduleHealthCheck();

        } catch (Exception e) {
            log.error("Error starting blockchain event listeners", e);
            scheduleReconnect();
        }
    }

    /**
     * Subscribes to AssetMinted events from the smart contract.
     * Saves mint events to the database and updates asset records.
     */
    private void listenToMintEvents() {
        try {
            log.info("Starting listener for AssetMinted events");

            // Define AssetMinted event: AssetMinted(string assetId, address issuer, uint256 amount, string assetType)
            Event assetMintedEvent = new Event(ASSET_MINTED_EVENT,
                Arrays.asList(
                    new TypeReference<Utf8String>(true) {}, // assetId (indexed)
                    new TypeReference<Address>(true) {},     // issuer (indexed)
                    new TypeReference<Uint256>() {},         // amount (non-indexed)
                    new TypeReference<Utf8String>() {}       // assetType (non-indexed)
                ));

            String eventSignature = EventEncoder.encode(assetMintedEvent);

            // Create filter for AssetMinted events
            EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
            ).addSingleTopic(eventSignature);

            // Subscribe to events
            Disposable subscription = web3j.ethLogFlowable(filter).subscribe(
                eventLog -> handleMintEvent(eventLog),
                error -> {
                    log.error("Error in AssetMinted event subscription", error);
                    scheduleReconnect();
                }
            );

            eventSubscriptions.put(ASSET_MINTED_EVENT, subscription);
            log.info("AssetMinted event listener registered successfully");

        } catch (Exception e) {
            log.error("Error setting up AssetMinted event listener", e);
            throw new RuntimeException("Failed to setup mint event listener", e);
        }
    }

    /**
     * Subscribes to AssetBurned events from the smart contract.
     * Saves burn events to the database and updates asset records.
     */
    private void listenToBurnEvents() {
        try {
            log.info("Starting listener for AssetBurned events");

            // Define AssetBurned event: AssetBurned(string assetId, address burner, uint256 amount)
            Event assetBurnedEvent = new Event(ASSET_BURNED_EVENT,
                Arrays.asList(
                    new TypeReference<Utf8String>(true) {}, // assetId (indexed)
                    new TypeReference<Address>(true) {},     // burner (indexed)
                    new TypeReference<Uint256>() {}          // amount (non-indexed)
                ));

            String eventSignature = EventEncoder.encode(assetBurnedEvent);

            // Create filter for AssetBurned events
            EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
            ).addSingleTopic(eventSignature);

            // Subscribe to events
            Disposable subscription = web3j.ethLogFlowable(filter).subscribe(
                eventLog -> handleBurnEvent(eventLog),
                error -> {
                    log.error("Error in AssetBurned event subscription", error);
                    scheduleReconnect();
                }
            );

            eventSubscriptions.put(ASSET_BURNED_EVENT, subscription);
            log.info("AssetBurned event listener registered successfully");

        } catch (Exception e) {
            log.error("Error setting up AssetBurned event listener", e);
            throw new RuntimeException("Failed to setup burn event listener", e);
        }
    }

    /**
     * Subscribes to Transfer events (ERC20 standard) from the smart contract.
     * Saves transfer events to the database and updates asset holder balances.
     */
    private void listenToTransferEvents() {
        try {
            log.info("Starting listener for Transfer events");

            // Define Transfer event: Transfer(address from, address to, uint256 value)
            Event transferEvent = new Event(TRANSFER_EVENT,
                Arrays.asList(
                    new TypeReference<Address>(true) {},  // from (indexed)
                    new TypeReference<Address>(true) {},  // to (indexed)
                    new TypeReference<Uint256>() {}       // value (non-indexed)
                ));

            String eventSignature = EventEncoder.encode(transferEvent);

            // Create filter for Transfer events
            EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
            ).addSingleTopic(eventSignature);

            // Subscribe to events
            Disposable subscription = web3j.ethLogFlowable(filter).subscribe(
                eventLog -> handleTransferEvent(eventLog),
                error -> {
                    log.error("Error in Transfer event subscription", error);
                    scheduleReconnect();
                }
            );

            eventSubscriptions.put(TRANSFER_EVENT, subscription);
            log.info("Transfer event listener registered successfully");

        } catch (Exception e) {
            log.error("Error setting up Transfer event listener", e);
            throw new RuntimeException("Failed to setup transfer event listener", e);
        }
    }

    /**
     * Replays past blockchain events from a specified block number.
     * Used for synchronizing database state with blockchain on startup.
     */
    @Transactional
    private void replayPastEvents() {
        try {
            log.info("Starting to replay past blockchain events from block: {}", replayFromBlock);

            // Get current block number
            BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("Current blockchain block number: {}", currentBlock);

            if (replayFromBlock >= currentBlock.longValue()) {
                log.info("No past events to replay - replay block is current or future");
                return;
            }

            // Create filter for all events from the contract
            EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(replayFromBlock)),
                DefaultBlockParameter.valueOf(currentBlock),
                contractAddress
            );

            // Fetch all past logs
            EthLog ethLog = web3j.ethGetLogs(filter).send();
            List<EthLog.LogResult> logs = ethLog.getLogs();

            log.info("Found {} past events to process", logs.size());

            int processedCount = 0;
            for (EthLog.LogResult logResult : logs) {
                try {
                    if (logResult instanceof EthLog.LogObject) {
                        Log eventLog = ((EthLog.LogObject) logResult).get();
                        processHistoricalEvent(eventLog);
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing historical event", e);
                    // Continue processing other events
                }
            }

            log.info("Successfully replayed {} past events", processedCount);

        } catch (Exception e) {
            log.error("Error replaying past events", e);
            throw new RuntimeException("Failed to replay past events", e);
        }
    }

    /**
     * Handles AssetMinted event and updates database.
     */
    @Transactional
    private void handleMintEvent(Log eventLog) {
        try {
            log.debug("Processing AssetMinted event - txHash: {}", eventLog.getTransactionHash());

            // Save blockchain event
            BlockchainEvent blockchainEvent = saveBlockchainEvent(eventLog, ASSET_MINTED_EVENT);

            // Parse event data (simplified - in production use FunctionReturnDecoder)
            String txHash = eventLog.getTransactionHash();
            String assetId = extractIndexedString(eventLog, 0);
            String issuer = extractIndexedAddress(eventLog, 1);
            BigInteger amount = extractUint256(eventLog);

            // Create or update asset
            Asset asset = assetRepository.findByAssetId(assetId).orElse(null);
            if (asset != null) {
                asset.setTotalSupply(asset.getTotalSupply().add(amount));
                assetRepository.save(asset);
                log.info("Updated asset {} total supply after mint", assetId);
            }

            // Create transaction record
            Transaction transaction = createTransaction(
                txHash, assetId, issuer, issuer, amount,
                TransactionType.MINT, eventLog
            );
            transactionRepository.save(transaction);

            // Update asset holder balance
            updateAssetHolderBalance(assetId, issuer, amount, true);

            // Mark event as processed
            blockchainEvent.setProcessed(true);
            blockchainEvent.setProcessedAt(LocalDateTime.now());
            blockchainEventRepository.save(blockchainEvent);

            log.info("AssetMinted event processed successfully - assetId: {}, amount: {}", assetId, amount);

        } catch (Exception e) {
            log.error("Error handling AssetMinted event", e);
        }
    }

    /**
     * Handles AssetBurned event and updates database.
     */
    @Transactional
    private void handleBurnEvent(Log eventLog) {
        try {
            log.debug("Processing AssetBurned event - txHash: {}", eventLog.getTransactionHash());

            // Save blockchain event
            BlockchainEvent blockchainEvent = saveBlockchainEvent(eventLog, ASSET_BURNED_EVENT);

            // Parse event data
            String txHash = eventLog.getTransactionHash();
            String assetId = extractIndexedString(eventLog, 0);
            String burner = extractIndexedAddress(eventLog, 1);
            BigInteger amount = extractUint256(eventLog);

            // Update asset total supply
            Asset asset = assetRepository.findByAssetId(assetId).orElse(null);
            if (asset != null) {
                asset.setTotalSupply(asset.getTotalSupply().subtract(amount));
                assetRepository.save(asset);
                log.info("Updated asset {} total supply after burn", assetId);
            }

            // Create transaction record
            Transaction transaction = createTransaction(
                txHash, assetId, burner, "0x0000000000000000000000000000000000000000",
                amount, TransactionType.BURN, eventLog
            );
            transactionRepository.save(transaction);

            // Update asset holder balance
            updateAssetHolderBalance(assetId, burner, amount, false);

            // Mark event as processed
            blockchainEvent.setProcessed(true);
            blockchainEvent.setProcessedAt(LocalDateTime.now());
            blockchainEventRepository.save(blockchainEvent);

            log.info("AssetBurned event processed successfully - assetId: {}, amount: {}", assetId, amount);

        } catch (Exception e) {
            log.error("Error handling AssetBurned event", e);
        }
    }

    /**
     * Handles Transfer event and updates database.
     */
    @Transactional
    private void handleTransferEvent(Log eventLog) {
        try {
            log.debug("Processing Transfer event - txHash: {}", eventLog.getTransactionHash());

            // Save blockchain event
            BlockchainEvent blockchainEvent = saveBlockchainEvent(eventLog, TRANSFER_EVENT);

            // Parse event data
            String txHash = eventLog.getTransactionHash();
            String from = extractIndexedAddress(eventLog, 0);
            String to = extractIndexedAddress(eventLog, 1);
            BigInteger amount = extractUint256(eventLog);

            // Determine asset ID from transaction (would need contract call in production)
            String assetId = "ASSET-" + eventLog.getTransactionHash().substring(2, 10);

            // Create transaction record
            Transaction transaction = createTransaction(
                txHash, assetId, from, to, amount,
                TransactionType.TRANSFER, eventLog
            );
            transactionRepository.save(transaction);

            // Update sender balance (decrease)
            if (!from.equals("0x0000000000000000000000000000000000000000")) {
                updateAssetHolderBalance(assetId, from, amount, false);
            }

            // Update receiver balance (increase)
            if (!to.equals("0x0000000000000000000000000000000000000000")) {
                updateAssetHolderBalance(assetId, to, amount, true);
            }

            // Mark event as processed
            blockchainEvent.setProcessed(true);
            blockchainEvent.setProcessedAt(LocalDateTime.now());
            blockchainEventRepository.save(blockchainEvent);

            log.info("Transfer event processed successfully - from: {}, to: {}, amount: {}", from, to, amount);

        } catch (Exception e) {
            log.error("Error handling Transfer event", e);
        }
    }

    /**
     * Processes historical events during replay.
     */
    @Transactional
    private void processHistoricalEvent(Log eventLog) {
        try {
            // Check if event already processed
            if (blockchainEventRepository.findByTransactionHashAndLogIndex(
                eventLog.getTransactionHash(), eventLog.getLogIndex().intValue()).isPresent()) {
                log.debug("Event already processed - skipping: {}", eventLog.getTransactionHash());
                return;
            }

            // Determine event type and handle accordingly
            List<String> topics = eventLog.getTopics();
            if (topics.isEmpty()) return;

            String eventSignature = topics.get(0);

            if (eventSignature.equals(EventEncoder.encode(createAssetMintedEvent()))) {
                handleMintEvent(eventLog);
            } else if (eventSignature.equals(EventEncoder.encode(createAssetBurnedEvent()))) {
                handleBurnEvent(eventLog);
            } else if (eventSignature.equals(EventEncoder.encode(createTransferEvent()))) {
                handleTransferEvent(eventLog);
            }

        } catch (Exception e) {
            log.error("Error processing historical event", e);
        }
    }

    /**
     * Saves a blockchain event to the database.
     */
    @Transactional
    private BlockchainEvent saveBlockchainEvent(Log eventLog, String eventName) {
        try {
            // Get block timestamp
            EthBlock.Block block = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(eventLog.getBlockNumber()), false
            ).send().getBlock();

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(block.getTimestamp().longValue()),
                ZoneId.systemDefault()
            );

            // Create event data JSON
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("topics", eventLog.getTopics());
            eventData.put("data", eventLog.getData());
            String eventDataJson = objectMapper.writeValueAsString(eventData);

            BlockchainEvent blockchainEvent = BlockchainEvent.builder()
                .eventName(eventName)
                .contractAddress(eventLog.getAddress())
                .blockNumber(eventLog.getBlockNumber().longValue())
                .transactionHash(eventLog.getTransactionHash())
                .logIndex(eventLog.getLogIndex().intValue())
                .eventData(eventDataJson)
                .processed(false)
                .timestamp(timestamp)
                .build();

            return blockchainEventRepository.save(blockchainEvent);

        } catch (Exception e) {
            log.error("Error saving blockchain event", e);
            throw new RuntimeException("Failed to save blockchain event", e);
        }
    }

    /**
     * Creates a transaction record.
     */
    private Transaction createTransaction(String txHash, String assetId, String from,
                                         String to, BigInteger amount,
                                         TransactionType type, Log eventLog) {
        try {
            EthBlock.Block block = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(eventLog.getBlockNumber()), false
            ).send().getBlock();

            LocalDateTime blockTimestamp = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(block.getTimestamp().longValue()),
                ZoneId.systemDefault()
            );

            return Transaction.builder()
                .transactionHash(txHash)
                .assetId(assetId)
                .fromAddress(from)
                .toAddress(to)
                .amount(amount)
                .transactionType(type)
                .blockNumber(eventLog.getBlockNumber().longValue())
                .blockTimestamp(blockTimestamp)
                .status(TransactionStatus.CONFIRMED)
                .build();

        } catch (Exception e) {
            log.error("Error creating transaction record", e);
            throw new RuntimeException("Failed to create transaction", e);
        }
    }

    /**
     * Updates asset holder balance.
     */
    @Transactional
    private void updateAssetHolderBalance(String assetId, String holderAddress,
                                         BigInteger amount, boolean isIncrease) {
        try {
            Optional<AssetHolder> existingHolder = assetHolderRepository
                .findByAssetIdAndHolderAddress(assetId, holderAddress);

            if (existingHolder.isPresent()) {
                AssetHolder holder = existingHolder.get();
                BigInteger newBalance = isIncrease
                    ? holder.getBalance().add(amount)
                    : holder.getBalance().subtract(amount);

                holder.setBalance(newBalance);
                assetHolderRepository.save(holder);
                log.debug("Updated balance for holder {} - new balance: {}", holderAddress, newBalance);
            } else if (isIncrease) {
                // Create new holder record
                AssetHolder newHolder = AssetHolder.builder()
                    .assetId(assetId)
                    .holderAddress(holderAddress)
                    .balance(amount)
                    .firstAcquired(LocalDateTime.now())
                    .build();
                assetHolderRepository.save(newHolder);
                log.debug("Created new asset holder record for {}", holderAddress);
            }

        } catch (Exception e) {
            log.error("Error updating asset holder balance", e);
        }
    }

    /**
     * Schedules a reconnection attempt after connection drop.
     */
    private void scheduleReconnect() {
        if (isListening) {
            log.info("Scheduling reconnection attempt in {} seconds", reconnectDelaySeconds);
            isListening = false;

            // Dispose existing subscriptions
            eventSubscriptions.values().forEach(Disposable::dispose);
            eventSubscriptions.clear();

            scheduler.schedule(() -> {
                try {
                    log.info("Attempting to reconnect event listeners");
                    startListening();
                } catch (Exception e) {
                    log.error("Reconnection failed", e);
                    scheduleReconnect();
                }
            }, reconnectDelaySeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * Schedules periodic health checks for event listeners.
     */
    private void scheduleHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isListening) {
                    log.debug("Event listener health check - Status: HEALTHY");
                } else {
                    log.warn("Event listener health check - Status: DISCONNECTED");
                }
            } catch (Exception e) {
                log.error("Error during health check", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    // Helper methods for event extraction (simplified - use proper ABI decoding in production)
    private String extractIndexedString(Log log, int index) {
        return log.getTopics().get(index + 1); // +1 because topic[0] is event signature
    }

    private String extractIndexedAddress(Log log, int index) {
        String topic = log.getTopics().get(index + 1);
        return "0x" + topic.substring(topic.length() - 40);
    }

    private BigInteger extractUint256(Log log) {
        return new BigInteger(log.getData().substring(2), 16);
    }

    private Event createAssetMintedEvent() {
        return new Event(ASSET_MINTED_EVENT,
            Arrays.asList(
                new TypeReference<Utf8String>(true) {},
                new TypeReference<Address>(true) {},
                new TypeReference<Uint256>() {},
                new TypeReference<Utf8String>() {}
            ));
    }

    private Event createAssetBurnedEvent() {
        return new Event(ASSET_BURNED_EVENT,
            Arrays.asList(
                new TypeReference<Utf8String>(true) {},
                new TypeReference<Address>(true) {},
                new TypeReference<Uint256>() {}
            ));
    }

    private Event createTransferEvent() {
        return new Event(TRANSFER_EVENT,
            Arrays.asList(
                new TypeReference<Address>(true) {},
                new TypeReference<Address>(true) {},
                new TypeReference<Uint256>() {}
            ));
    }
}