package com.enterprise.tokenization.repository;

import com.enterprise.tokenization.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Asset entity operations.
 * Provides CRUD operations and custom query methods for asset management.
 */
@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Find an asset by its unique asset ID.
     *
     * @param assetId the asset ID to search for
     * @return Optional containing the asset if found, empty otherwise
     */
    Optional<Asset> findByAssetId(String assetId);

    /**
     * Find all assets of a specific type.
     *
     * @param assetType the asset type to filter by
     * @return List of assets matching the specified type
     */
    List<Asset> findByAssetType(String assetType);

    /**
     * Check if an asset exists with the given asset ID.
     *
     * @param assetId the asset ID to check
     * @return true if an asset exists with the asset ID, false otherwise
     */
    boolean existsByAssetId(String assetId);

    /**
     * Find an asset by its smart contract address.
     *
     * @param contractAddress the contract address to search for
     * @return Optional containing the asset if found, empty otherwise
     */
    Optional<Asset> findByContractAddress(String contractAddress);
}