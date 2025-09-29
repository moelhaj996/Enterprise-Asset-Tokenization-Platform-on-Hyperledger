# Quick Start Guide

Get up and running with the Enterprise Asset Tokenization Platform in under 10 minutes.

## Prerequisites

Ensure you have installed:
- Docker & Docker Compose
- Node.js (18+)
- Git

## 5-Minute Setup

### 1. Clone and Configure

```bash
# Clone repository
git clone <repository-url>
cd Enterprise-Asset-Tokenization-Platform-on-Hyperledger

# Setup environment
cp .env.example .env
# Edit JWT_SECRET if needed (optional for testing)
```

### 2. Install Dependencies

```bash
cd contracts
npm install
cd ..
```

### 3. Start All Services

```bash
docker-compose up -d
```

Wait ~60 seconds for all services to be healthy.

### 4. Deploy Smart Contract

```bash
cd contracts
npx hardhat compile
npx hardhat run scripts/deploy.js --network besu
```

**Note the contract address from output!**

### 5. Update Contract Address

```bash
# Update .env file
echo "ASSET_TOKEN_CONTRACT_ADDRESS=<your-contract-address>" >> .env

# Restart backend
docker-compose restart backend
```

### 6. Test the API

```bash
# Get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Mint an asset
curl -X POST http://localhost:8080/api/assets/mint \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "assetId": "BOND-2024-001",
    "assetType": "CORPORATE_BOND",
    "amount": 1000000,
    "recipient": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
    "issuer": "Acme Corp",
    "metadata": {"maturityDate": "2029-12-31", "couponRate": 5.5}
  }'

# Check blockchain status
curl http://localhost:8080/api/blockchain/status
```

## Access Points

- **API:** http://localhost:8080/api
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **Besu RPC:** http://localhost:8545

## Default Credentials

**Admin User:**
- Username: `admin`
- Password: `admin123`
- Address: `0xfe3b557e8fb62b89f4916b721be55ceb828dbd73`

**Minter User:**
- Username: `minter`
- Password: `admin123`
- Address: `0x627306090abaB3A6e1400e9345bC60c78a8BEf57`

**Burner User:**
- Username: `burner`
- Password: `admin123`
- Address: `0xf17f52151EbEF6C7334FAD080c5704D77216b732`

## Common Commands

### Service Management

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Restart backend
docker-compose restart backend

# Check status
docker-compose ps
```

### Contract Development

```bash
cd contracts

# Compile
npx hardhat compile

# Test
npx hardhat test

# Coverage
npx hardhat coverage

# Deploy
npx hardhat run scripts/deploy.js --network besu
```

### Backend Development

```bash
cd backend

# Build
mvn clean package

# Test
mvn test

# Run locally
mvn spring-boot:run
```

## Troubleshooting

### Services won't start
```bash
docker-compose down -v
docker-compose up -d
```

### Contract deployment fails
```bash
# Check Besu is running
curl http://localhost:8545

# Try deployment again
cd contracts
npx hardhat run scripts/deploy.js --network besu
```

### Backend can't connect
```bash
# Verify .env configuration
cat .env | grep BESU_RPC_URL
cat .env | grep CONTRACT

# Restart backend
docker-compose restart backend
```

## Next Steps

1. **Explore API:** Open http://localhost:8080/swagger-ui.html
2. **Read Docs:** See README.md and docs/ folder
3. **Run Tests:** Execute smart contract and backend tests
4. **Customize:** Modify smart contracts for your use case

## Support

- **Documentation:** README.md, docs/ARCHITECTURE.md, docs/API.md
- **Issues:** Create a GitHub issue
- **Examples:** See docs/API.md for request examples

---

**Happy Tokenizing! 🚀**