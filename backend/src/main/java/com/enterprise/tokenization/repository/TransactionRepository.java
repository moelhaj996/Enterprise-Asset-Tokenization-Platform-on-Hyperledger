package com.enterprise.tokenization.repository;

import com.enterprise.tokenization.model.Transaction;
import com.enterprise.tokenization.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Transaction entity operations.
 * Provides CRUD operations and custom query methods for transaction management.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find a transaction by its unique transaction hash.
     *
     * @param hash the transaction hash to search for
     * @return Optional containing the transaction if found, empty otherwise
     */
    Optional<Transaction> findByTransactionHash(String hash);

    /**
     * Find all transactions associated with a specific asset.
     *
     * @param assetId the asset ID to filter by
     * @return List of transactions for the specified asset
     */
    List<Transaction> findByAssetId(String assetId);

    /**
     * Find all transactions originating from a specific address.
     *
     * @param address the from address to filter by
     * @return List of transactions from the specified address
     */
    List<Transaction> findByFromAddress(String address);

    /**
     * Find all transactions sent to a specific address.
     *
     * @param address the to address to filter by
     * @return List of transactions to the specified address
     */
    List<Transaction> findByToAddress(String address);

    /**
     * Find all transactions with a specific status.
     *
     * @param status the transaction status to filter by
     * @return List of transactions with the specified status
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Find recent transactions ordered by block timestamp in descending order.
     * Supports pagination for efficient data retrieval.
     *
     * @param pageable pagination parameters
     * @return Page of recent transactions
     */
    @Query("SELECT t FROM Transaction t ORDER BY t.blockTimestamp DESC")
    Page<Transaction> findRecentTransactions(Pageable pageable);

    /**
     * Find recent transactions for a specific asset ordered by block timestamp.
     *
     * @param assetId the asset ID to filter by
     * @param pageable pagination parameters
     * @return Page of recent transactions for the asset
     */
    @Query("SELECT t FROM Transaction t WHERE t.assetId = :assetId ORDER BY t.blockTimestamp DESC")
    Page<Transaction> findRecentTransactionsByAsset(@Param("assetId") String assetId, Pageable pageable);
}