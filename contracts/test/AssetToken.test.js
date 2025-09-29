const { expect } = require("chai");
const { ethers } = require("hardhat");
const { loadFixture } = require("@nomicfoundation/hardhat-network-helpers");

describe("AssetToken", function () {
  // Fixture to deploy contract
  async function deployAssetTokenFixture() {
    const [admin, minter, burner, pauser, user1, user2] = await ethers.getSigners();

    const AssetToken = await ethers.getContractFactory("AssetToken");
    const token = await AssetToken.deploy("Asset Token", "AST", admin.address);

    // Grant roles
    const MINTER_ROLE = await token.MINTER_ROLE();
    const BURNER_ROLE = await token.BURNER_ROLE();
    const PAUSER_ROLE = await token.PAUSER_ROLE();

    await token.connect(admin).grantRole(MINTER_ROLE, minter.address);
    await token.connect(admin).grantRole(BURNER_ROLE, burner.address);
    await token.connect(admin).grantRole(PAUSER_ROLE, pauser.address);

    return { token, admin, minter, burner, pauser, user1, user2, MINTER_ROLE, BURNER_ROLE, PAUSER_ROLE };
  }

  describe("Deployment", function () {
    it("Should set the correct token name and symbol", async function () {
      const { token } = await loadFixture(deployAssetTokenFixture);
      expect(await token.name()).to.equal("Asset Token");
      expect(await token.symbol()).to.equal("AST");
    });

    it("Should grant admin all roles initially", async function () {
      const { token, admin, MINTER_ROLE, BURNER_ROLE, PAUSER_ROLE } = await loadFixture(deployAssetTokenFixture);

      const DEFAULT_ADMIN_ROLE = await token.DEFAULT_ADMIN_ROLE();
      expect(await token.hasRole(DEFAULT_ADMIN_ROLE, admin.address)).to.be.true;
      expect(await token.hasRole(MINTER_ROLE, admin.address)).to.be.true;
      expect(await token.hasRole(BURNER_ROLE, admin.address)).to.be.true;
      expect(await token.hasRole(PAUSER_ROLE, admin.address)).to.be.true;
    });

    it("Should revert if admin is zero address", async function () {
      const AssetToken = await ethers.getContractFactory("AssetToken");
      await expect(
        AssetToken.deploy("Asset Token", "AST", ethers.ZeroAddress)
      ).to.be.revertedWith("AssetToken: admin is zero address");
    });
  });

  describe("Minting", function () {
    it("Should allow MINTER to mint tokens", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      const amount = ethers.parseEther("1000");
      await expect(
        token.connect(minter).mint(
          user1.address,
          amount,
          "BOND-2024-001",
          "CORPORATE_BOND",
          "Acme Corp"
        )
      ).to.emit(token, "AssetMinted")
        .withArgs(user1.address, amount, "BOND-2024-001", "CORPORATE_BOND", await ethers.provider.getBlock('latest').then(b => b.timestamp + 1));

      expect(await token.balanceOf(user1.address)).to.equal(amount);
    });

    it("Should store asset metadata correctly", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      const metadata = await token.getAssetMetadata("BOND-2024-001");
      expect(metadata.assetId).to.equal("BOND-2024-001");
      expect(metadata.assetType).to.equal("CORPORATE_BOND");
      expect(metadata.issuer).to.equal("Acme Corp");
      expect(metadata.exists).to.be.true;
    });

    it("Should revert if non-MINTER tries to mint", async function () {
      const { token, user1, user2 } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(user1).mint(
          user2.address,
          ethers.parseEther("1000"),
          "BOND-2024-001",
          "CORPORATE_BOND",
          "Acme Corp"
        )
      ).to.be.reverted;
    });

    it("Should revert if minting to zero address", async function () {
      const { token, minter } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(minter).mint(
          ethers.ZeroAddress,
          ethers.parseEther("1000"),
          "BOND-2024-001",
          "CORPORATE_BOND",
          "Acme Corp"
        )
      ).to.be.revertedWith("AssetToken: mint to zero address");
    });

    it("Should revert if minting zero amount", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(minter).mint(
          user1.address,
          0,
          "BOND-2024-001",
          "CORPORATE_BOND",
          "Acme Corp"
        )
      ).to.be.revertedWith("AssetToken: mint amount must be greater than zero");
    });

    it("Should revert if asset ID is empty", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(minter).mint(
          user1.address,
          ethers.parseEther("1000"),
          "",
          "CORPORATE_BOND",
          "Acme Corp"
        )
      ).to.be.revertedWith("AssetToken: asset ID cannot be empty");
    });

    it("Should revert if asset type is empty", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(minter).mint(
          user1.address,
          ethers.parseEther("1000"),
          "BOND-2024-001",
          "",
          "Acme Corp"
        )
      ).to.be.revertedWith("AssetToken: asset type cannot be empty");
    });

    it("Should allow minting multiple assets", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("500"),
        "INVOICE-2024-001",
        "INVOICE",
        "Beta LLC"
      );

      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("1500"));

      const allAssets = await token.getAllAssetIds();
      expect(allAssets.length).to.equal(2);
      expect(allAssets[0]).to.equal("BOND-2024-001");
      expect(allAssets[1]).to.equal("INVOICE-2024-001");
    });
  });

  describe("Burning", function () {
    it("Should allow BURNER to burn tokens", async function () {
      const { token, minter, burner, user1 } = await loadFixture(deployAssetTokenFixture);

      const mintAmount = ethers.parseEther("1000");
      const burnAmount = ethers.parseEther("300");

      await token.connect(minter).mint(
        user1.address,
        mintAmount,
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await expect(
        token.connect(burner).burn(user1.address, burnAmount, "BOND-2024-001")
      ).to.emit(token, "AssetBurned")
        .withArgs(user1.address, burnAmount, "BOND-2024-001", await ethers.provider.getBlock('latest').then(b => b.timestamp + 1));

      expect(await token.balanceOf(user1.address)).to.equal(mintAmount - burnAmount);
    });

    it("Should revert if non-BURNER tries to burn", async function () {
      const { token, minter, user1, user2 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await expect(
        token.connect(user2).burn(user1.address, ethers.parseEther("100"), "BOND-2024-001")
      ).to.be.reverted;
    });

    it("Should revert if burning from zero address", async function () {
      const { token, minter, burner, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await expect(
        token.connect(burner).burn(ethers.ZeroAddress, ethers.parseEther("100"), "BOND-2024-001")
      ).to.be.revertedWith("AssetToken: burn from zero address");
    });

    it("Should revert if burning zero amount", async function () {
      const { token, minter, burner, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await expect(
        token.connect(burner).burn(user1.address, 0, "BOND-2024-001")
      ).to.be.revertedWith("AssetToken: burn amount must be greater than zero");
    });

    it("Should revert if asset does not exist", async function () {
      const { token, burner, user1 } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(burner).burn(user1.address, ethers.parseEther("100"), "NONEXISTENT")
      ).to.be.revertedWith("AssetToken: asset does not exist");
    });

    it("Should revert if burning more than balance", async function () {
      const { token, minter, burner, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await expect(
        token.connect(burner).burn(user1.address, ethers.parseEther("2000"), "BOND-2024-001")
      ).to.be.reverted;
    });
  });

  describe("Pausing", function () {
    it("Should allow PAUSER to pause the contract", async function () {
      const { token, pauser } = await loadFixture(deployAssetTokenFixture);

      await token.connect(pauser).pause();
      expect(await token.paused()).to.be.true;
    });

    it("Should allow PAUSER to unpause the contract", async function () {
      const { token, pauser } = await loadFixture(deployAssetTokenFixture);

      await token.connect(pauser).pause();
      await token.connect(pauser).unpause();
      expect(await token.paused()).to.be.false;
    });

    it("Should revert if non-PAUSER tries to pause", async function () {
      const { token, user1 } = await loadFixture(deployAssetTokenFixture);

      await expect(token.connect(user1).pause()).to.be.reverted;
    });

    it("Should prevent minting when paused", async function () {
      const { token, minter, pauser, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(pauser).pause();

      await expect(
        token.connect(minter).mint(
          user1.address,
          ethers.parseEther("1000"),
          "BOND-2024-001",
          "CORPORATE_BOND",
          "Acme Corp"
        )
      ).to.be.reverted;
    });

    it("Should prevent burning when paused", async function () {
      const { token, minter, burner, pauser, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await token.connect(pauser).pause();

      await expect(
        token.connect(burner).burn(user1.address, ethers.parseEther("100"), "BOND-2024-001")
      ).to.be.reverted;
    });

    it("Should prevent transfers when paused", async function () {
      const { token, minter, pauser, user1, user2 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await token.connect(pauser).pause();

      await expect(
        token.connect(user1).transfer(user2.address, ethers.parseEther("100"))
      ).to.be.reverted;
    });
  });

  describe("Role Management", function () {
    it("Should allow admin to grant roles", async function () {
      const { token, admin, user1, MINTER_ROLE } = await loadFixture(deployAssetTokenFixture);

      await token.connect(admin).grantRole(MINTER_ROLE, user1.address);
      expect(await token.hasRole(MINTER_ROLE, user1.address)).to.be.true;
    });

    it("Should allow admin to revoke roles", async function () {
      const { token, admin, minter, MINTER_ROLE } = await loadFixture(deployAssetTokenFixture);

      await token.connect(admin).revokeRole(MINTER_ROLE, minter.address);
      expect(await token.hasRole(MINTER_ROLE, minter.address)).to.be.false;
    });

    it("Should revert if non-admin tries to grant roles", async function () {
      const { token, user1, user2, MINTER_ROLE } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.connect(user1).grantRole(MINTER_ROLE, user2.address)
      ).to.be.reverted;
    });
  });

  describe("Asset Queries", function () {
    it("Should return asset metadata correctly", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      const metadata = await token.getAssetMetadata("BOND-2024-001");
      expect(metadata.assetId).to.equal("BOND-2024-001");
      expect(metadata.assetType).to.equal("CORPORATE_BOND");
      expect(metadata.issuer).to.equal("Acme Corp");
      expect(metadata.exists).to.be.true;
      expect(metadata.issuanceDate).to.be.gt(0);
    });

    it("Should revert when querying non-existent asset", async function () {
      const { token } = await loadFixture(deployAssetTokenFixture);

      await expect(
        token.getAssetMetadata("NONEXISTENT")
      ).to.be.revertedWith("AssetToken: asset does not exist");
    });

    it("Should return all asset IDs", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("500"),
        "INVOICE-2024-001",
        "INVOICE",
        "Beta LLC"
      );

      const allAssets = await token.getAllAssetIds();
      expect(allAssets.length).to.equal(2);
      expect(allAssets).to.include("BOND-2024-001");
      expect(allAssets).to.include("INVOICE-2024-001");
    });

    it("Should return holder assets correctly", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      const holderAssets = await token.getHolderAssets(user1.address);
      expect(holderAssets.length).to.equal(1);
      expect(holderAssets[0]).to.equal("BOND-2024-001");
    });

    it("Should check asset existence correctly", async function () {
      const { token, minter, user1 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      expect(await token.assetExists("BOND-2024-001")).to.be.true;
      expect(await token.assetExists("NONEXISTENT")).to.be.false;
    });
  });

  describe("Transfers", function () {
    it("Should allow token transfers", async function () {
      const { token, minter, user1, user2 } = await loadFixture(deployAssetTokenFixture);

      await token.connect(minter).mint(
        user1.address,
        ethers.parseEther("1000"),
        "BOND-2024-001",
        "CORPORATE_BOND",
        "Acme Corp"
      );

      await token.connect(user1).transfer(user2.address, ethers.parseEther("300"));

      expect(await token.balanceOf(user1.address)).to.equal(ethers.parseEther("700"));
      expect(await token.balanceOf(user2.address)).to.equal(ethers.parseEther("300"));
    });
  });
});