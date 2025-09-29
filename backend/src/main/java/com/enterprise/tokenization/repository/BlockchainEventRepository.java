package com.enterprise.tokenization.repository;

import com.enterprise.tokenization.model.BlockchainEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for BlockchainEvent entity operations.
 * Provides CRUD operations and custom query methods for blockchain event management.
 */
@Repository
public interface BlockchainEventRepository extends JpaRepository<BlockchainEvent, Long> {

    /**
     * Find all unprocessed blockchain events.
     * Useful for event processing queues and background jobs.
     *
     * @return List of blockchain events that have not been processed
     */
    List<BlockchainEvent> findByProcessedFalse();

    /**
     * Find all blockchain events by event name.
     *
     * @param eventName the event name to filter by
     * @return List of blockchain events with the specified event name
     */
    List<BlockchainEvent> findByEventName(String eventName);

    /**
     * Find all blockchain events from a specific smart contract address.
     *
     * @param address the contract address to filter by
     * @return List of blockchain events from the specified contract
     */
    List<BlockchainEvent> findByContractAddress(String address);

    /**
     * Find a specific blockchain event by transaction hash and log index.
     * This combination uniquely identifies an event within a transaction.
     *
     * @param hash the transaction hash
     * @param logIndex the log index within the transaction
     * @return Optional containing the blockchain event if found, empty otherwise
     */
    Optional<BlockchainEvent> findByTransactionHashAndLogIndex(String hash, Integer logIndex);
}