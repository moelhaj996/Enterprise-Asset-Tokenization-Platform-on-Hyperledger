const hre = require("hardhat");

async function main() {
  console.log("Deploying AssetToken contract...");

  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying with account:", deployer.address);

  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("Account balance:", hre.ethers.formatEther(balance), "ETH");

  // Deploy AssetToken
  const AssetToken = await hre.ethers.getContractFactory("AssetToken");
  const token = await AssetToken.deploy(
    "Enterprise Asset Token",
    "EAT",
    deployer.address
  );

  await token.waitForDeployment();

  const tokenAddress = await token.getAddress();
  console.log("AssetToken deployed to:", tokenAddress);

  // Save deployment info
  const deploymentInfo = {
    network: hre.network.name,
    contractAddress: tokenAddress,
    deployer: deployer.address,
    timestamp: new Date().toISOString(),
    tokenName: "Enterprise Asset Token",
    tokenSymbol: "EAT"
  };

  console.log("\n=== Deployment Summary ===");
  console.log(JSON.stringify(deploymentInfo, null, 2));

  console.log("\n=== Update your .env file with: ===");
  console.log(`ASSET_TOKEN_CONTRACT_ADDRESS=${tokenAddress}`);

  // Verify roles
  const DEFAULT_ADMIN_ROLE = await token.DEFAULT_ADMIN_ROLE();
  const MINTER_ROLE = await token.MINTER_ROLE();
  const BURNER_ROLE = await token.BURNER_ROLE();
  const PAUSER_ROLE = await token.PAUSER_ROLE();

  console.log("\n=== Role Configuration ===");
  console.log("Admin has DEFAULT_ADMIN_ROLE:", await token.hasRole(DEFAULT_ADMIN_ROLE, deployer.address));
  console.log("Admin has MINTER_ROLE:", await token.hasRole(MINTER_ROLE, deployer.address));
  console.log("Admin has BURNER_ROLE:", await token.hasRole(BURNER_ROLE, deployer.address));
  console.log("Admin has PAUSER_ROLE:", await token.hasRole(PAUSER_ROLE, deployer.address));
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });