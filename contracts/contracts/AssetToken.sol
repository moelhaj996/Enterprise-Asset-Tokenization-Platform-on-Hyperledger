// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Pausable.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * @title AssetToken
 * @dev ERC20 token for tokenizing real-world assets with role-based access control
 * @notice This contract implements asset tokenization for bonds, invoices, and supply chain assets
 */
contract AssetToken is ERC20, ERC20Burnable, ERC20Pausable, AccessControl, ReentrancyGuard {
    // Role definitions
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");

    // Asset metadata structure
    struct AssetMetadata {
        string assetId;
        string assetType;
        uint256 issuanceDate;
        string issuer;
        bool exists;
    }

    // Mapping from asset ID to metadata
    mapping(string => AssetMetadata) private _assetMetadata;

    // Mapping from address to list of asset IDs they hold
    mapping(address => string[]) private _holderAssets;

    // Array of all asset IDs
    string[] private _allAssetIds;

    // Events
    event AssetMinted(
        address indexed to,
        uint256 amount,
        string assetId,
        string assetType,
        uint256 timestamp
    );

    event AssetBurned(
        address indexed from,
        uint256 amount,
        string assetId,
        uint256 timestamp
    );

    event AssetMetadataUpdated(
        string assetId,
        string assetType,
        string issuer,
        uint256 timestamp
    );

    /**
     * @dev Constructor that sets up the token with initial admin
     * @param name Token name
     * @param symbol Token symbol
     * @param admin Address of the initial admin
     */
    constructor(
        string memory name,
        string memory symbol,
        address admin
    ) ERC20(name, symbol) {
        require(admin != address(0), "AssetToken: admin is zero address");

        _grantRole(DEFAULT_ADMIN_ROLE, admin);
        _grantRole(MINTER_ROLE, admin);
        _grantRole(BURNER_ROLE, admin);
        _grantRole(PAUSER_ROLE, admin);
    }

    /**
     * @dev Mints new asset tokens
     * @param to Address to receive the minted tokens
     * @param amount Amount of tokens to mint
     * @param assetId Unique identifier for the asset
     * @param assetType Type of asset (e.g., "CORPORATE_BOND", "INVOICE")
     * @param issuer Name of the asset issuer
     */
    function mint(
        address to,
        uint256 amount,
        string memory assetId,
        string memory assetType,
        string memory issuer
    ) external onlyRole(MINTER_ROLE) nonReentrant whenNotPaused {
        require(to != address(0), "AssetToken: mint to zero address");
        require(amount > 0, "AssetToken: mint amount must be greater than zero");
        require(bytes(assetId).length > 0, "AssetToken: asset ID cannot be empty");
        require(bytes(assetType).length > 0, "AssetToken: asset type cannot be empty");

        // Store or update asset metadata
        if (!_assetMetadata[assetId].exists) {
            _assetMetadata[assetId] = AssetMetadata({
                assetId: assetId,
                assetType: assetType,
                issuanceDate: block.timestamp,
                issuer: issuer,
                exists: true
            });
            _allAssetIds.push(assetId);
            _holderAssets[to].push(assetId);
        }

        _mint(to, amount);

        emit AssetMinted(to, amount, assetId, assetType, block.timestamp);
    }

    /**
     * @dev Burns asset tokens
     * @param from Address to burn tokens from
     * @param amount Amount of tokens to burn
     * @param assetId Unique identifier for the asset
     */
    function burn(
        address from,
        uint256 amount,
        string memory assetId
    ) external onlyRole(BURNER_ROLE) nonReentrant whenNotPaused {
        require(from != address(0), "AssetToken: burn from zero address");
        require(amount > 0, "AssetToken: burn amount must be greater than zero");
        require(_assetMetadata[assetId].exists, "AssetToken: asset does not exist");

        _burn(from, amount);

        emit AssetBurned(from, amount, assetId, block.timestamp);
    }

    /**
     * @dev Pauses all token transfers
     * @notice Can only be called by addresses with PAUSER_ROLE
     */
    function pause() external onlyRole(PAUSER_ROLE) {
        _pause();
    }

    /**
     * @dev Unpauses all token transfers
     * @notice Can only be called by addresses with PAUSER_ROLE
     */
    function unpause() external onlyRole(PAUSER_ROLE) {
        _unpause();
    }

    /**
     * @dev Returns asset metadata for a given asset ID
     * @param assetId The asset ID to query
     * @return AssetMetadata structure containing asset details
     */
    function getAssetMetadata(string memory assetId)
        external
        view
        returns (AssetMetadata memory)
    {
        require(_assetMetadata[assetId].exists, "AssetToken: asset does not exist");
        return _assetMetadata[assetId];
    }

    /**
     * @dev Returns all asset IDs
     * @return Array of all asset IDs
     */
    function getAllAssetIds() external view returns (string[] memory) {
        return _allAssetIds;
    }

    /**
     * @dev Returns asset IDs held by a specific address
     * @param holder The address to query
     * @return Array of asset IDs held by the address
     */
    function getHolderAssets(address holder) external view returns (string[] memory) {
        return _holderAssets[holder];
    }

    /**
     * @dev Checks if an asset exists
     * @param assetId The asset ID to check
     * @return Boolean indicating if the asset exists
     */
    function assetExists(string memory assetId) external view returns (bool) {
        return _assetMetadata[assetId].exists;
    }

    /**
     * @dev Override required by Solidity for multiple inheritance
     */
    function _update(address from, address to, uint256 value)
        internal
        override(ERC20, ERC20Pausable)
    {
        super._update(from, to, value);
    }
}