package com.enterprise.tokenization.repository;

import com.enterprise.tokenization.model.AssetHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AssetHolder entity operations.
 * Provides CRUD operations and custom query methods for asset holder (token balance) management.
 */
@Repository
public interface AssetHolderRepository extends JpaRepository<AssetHolder, Long> {

    /**
     * Find all holders of a specific asset.
     *
     * @param assetId the asset ID to filter by
     * @return List of asset holders for the specified asset
     */
    List<AssetHolder> findByAssetId(String assetId);

    /**
     * Find all assets held by a specific address.
     *
     * @param address the holder address to filter by
     * @return List of asset holdings for the specified address
     */
    List<AssetHolder> findByHolderAddress(String address);

    /**
     * Find a specific asset holder record by asset ID and holder address.
     * This combination uniquely identifies a holder's balance for an asset.
     *
     * @param assetId the asset ID
     * @param address the holder address
     * @return Optional containing the asset holder if found, empty otherwise
     */
    Optional<AssetHolder> findByAssetIdAndHolderAddress(String assetId, String address);
}