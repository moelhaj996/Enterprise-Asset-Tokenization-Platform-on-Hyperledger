# Enterprise Asset Tokenization Platform on Hyperledger Besu

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)
![Solidity](https://img.shields.io/badge/Solidity-0.8.20-363636.svg)
![Hyperledger Besu](https://img.shields.io/badge/Hyperledger%20Besu-Latest-2F3134.svg)

A production-ready blockchain solution for tokenizing real-world assets (bonds, invoices, supply chain assets) on a permissioned Hyperledger Besu network. This enterprise-grade platform demonstrates blockchain integration capabilities suitable for regulated financial environments.

## 🎯 Features

- ✅ **Permissioned Blockchain**: 4-node Hyperledger Besu network with IBFT 2.0 consensus
- ✅ **Smart Contracts**: ERC-20 compliant AssetToken with role-based access control
- ✅ **Enterprise Backend**: Java Spring Boot API with Web3j integration
- ✅ **Database Integration**: PostgreSQL for off-chain data and audit trails
- ✅ **JWT Authentication**: Secure API access with role-based authorization
- ✅ **Event Monitoring**: Real-time blockchain event listener with database synchronization
- ✅ **Docker Deployment**: Complete containerized deployment with Docker Compose
- ✅ **CI/CD Pipeline**: Automated testing and deployment with GitHub Actions
- ✅ **API Documentation**: Interactive Swagger/OpenAPI documentation

## 🏗️ Architecture

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   Frontend      │─────▶│  Java Spring     │─────▶│  Hyperledger    │
│   (Optional)    │      │  Boot API        │      │  Besu Network   │
│   React/Next.js │◀─────│  + Web3j         │◀─────│  (4 Validators) │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                │                           │
                                ▼                           │
                         ┌──────────────┐                  │
                         │  PostgreSQL  │                  │
                         │  Database    │                  │
                         └──────────────┘                  │
                                                            │
                         ┌──────────────────────────────────┘
                         │
                         ▼
                  ┌──────────────────┐
                  │ Smart Contracts  │
                  │ - AssetToken.sol │
                  │ - AccessControl  │
                  └──────────────────┘
```

## 🛠️ Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Blockchain** | Hyperledger Besu | Latest | Private permissioned network |
| **Consensus** | IBFT 2.0 | - | Byzantine fault tolerance |
| **Smart Contracts** | Solidity | 0.8.20 | Asset tokenization logic |
| **Backend** | Java + Spring Boot | 17 / 3.2 | REST API and business logic |
| **Blockchain Library** | Web3j | 4.10.3 | Java-Ethereum interaction |
| **Database** | PostgreSQL | 15 | Off-chain data storage |
| **Authentication** | JWT | - | Secure API access |
| **Containerization** | Docker Compose | Latest | Multi-service deployment |
| **CI/CD** | GitHub Actions | - | Automated testing |
| **Testing** | JUnit 5, Hardhat | - | Comprehensive test coverage |

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Docker** (20.10+) and **Docker Compose** (2.0+)
- **Java** (17+) - for local development
- **Node.js** (18+) and **npm** - for smart contract development
- **Git** - for version control
- **Maven** (3.8+) - for Java builds

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Enterprise-Asset-Tokenization-Platform-on-Hyperledger
```

### 2. Configure Environment Variables

```bash
cp .env.example .env
# Edit .env file with your configurations
nano .env
```

**Important:** Update the following in `.env`:
- `JWT_SECRET` - Use a strong 256-bit secret key
- `DB_PASSWORD` - Set a secure database password
- `DEPLOYER_PRIVATE_KEY` - Use the provided test key or your own

### 3. Install Smart Contract Dependencies

```bash
cd contracts
npm install
cd ..
```

### 4. Start the Besu Network and Services

```bash
# Start all services (Besu nodes, PostgreSQL, backend)
docker-compose up -d

# Wait for services to be healthy (this may take 60-90 seconds)
docker-compose ps
```

### 5. Deploy Smart Contracts

```bash
cd contracts

# Compile contracts
npx hardhat compile

# Deploy to Besu network
npx hardhat run scripts/deploy.js --network besu

# Note the deployed contract address and update .env
# ASSET_TOKEN_CONTRACT_ADDRESS=0x...
```

### 6. Restart Backend with Contract Address

```bash
# Update .env with contract address, then restart backend
docker-compose restart backend
```

### 7. Access the Application

- **API Base URL**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Besu RPC**: http://localhost:8545

### 8. Test the API

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Mint an asset (use JWT token from login response)
curl -X POST http://localhost:8080/api/assets/mint \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "assetId": "BOND-2024-001",
    "assetType": "CORPORATE_BOND",
    "amount": 1000000,
    "recipient": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
    "issuer": "Acme Corp",
    "metadata": {
      "maturityDate": "2029-12-31",
      "couponRate": 5.5
    }
  }'

# Check blockchain status
curl http://localhost:8080/api/blockchain/status
```

## 📁 Project Structure

```
Enterprise-Asset-Tokenization-Platform/
├── contracts/                    # Smart contracts (Solidity)
│   ├── contracts/
│   │   └── AssetToken.sol       # Main asset tokenization contract
│   ├── test/
│   │   └── AssetToken.test.js   # Hardhat tests
│   ├── scripts/
│   │   └── deploy.js            # Deployment script
│   ├── hardhat.config.js
│   └── package.json
│
├── backend/                      # Java Spring Boot backend
│   ├── src/main/java/com/enterprise/tokenization/
│   │   ├── TokenizationApplication.java
│   │   ├── blockchain/          # Web3j blockchain services
│   │   ├── config/              # Spring configuration
│   │   ├── controller/          # REST controllers
│   │   ├── dto/                 # Data transfer objects
│   │   ├── exception/           # Exception handling
│   │   ├── model/               # JPA entities
│   │   ├── repository/          # Spring Data repositories
│   │   ├── security/            # JWT security
│   │   └── service/             # Business logic services
│   ├── src/main/resources/
│   │   ├── application.yml      # Application configuration
│   │   └── db/migration/        # Flyway migrations
│   ├── Dockerfile
│   └── pom.xml
│
├── config/                       # Configuration files
│   └── besu/
│       └── genesis.json         # Besu genesis configuration
│
├── docs/                         # Documentation
│
├── .github/workflows/
│   └── ci-cd.yml                # GitHub Actions CI/CD
│
├── docker-compose.yml            # Multi-service orchestration
├── .env.example                  # Environment variables template
├── .gitignore
└── README.md
```

## 🔐 Security Features

### Smart Contract Security
- ✅ OpenZeppelin's battle-tested libraries
- ✅ Role-based access control (RBAC)
- ✅ Emergency pause functionality
- ✅ Input validation and zero-address checks
- ✅ Reentrancy protection
- ✅ Comprehensive test coverage (80%+)

### API Security
- ✅ JWT-based authentication
- ✅ BCrypt password hashing
- ✅ Role-based authorization
- ✅ CORS configuration
- ✅ Input validation with Jakarta Bean Validation
- ✅ SQL injection prevention (parameterized queries)

### Infrastructure Security
- ✅ Non-root Docker containers
- ✅ Private keys in environment variables (not in code)
- ✅ Database credentials externalized
- ✅ Network isolation with Docker bridge network
- ✅ Health checks for all services

## 🧪 Testing

### Smart Contract Tests

```bash
cd contracts

# Run all tests
npx hardhat test

# Run tests with coverage
npx hardhat coverage

# Run specific test file
npx hardhat test test/AssetToken.test.js

# Generate gas report
REPORT_GAS=true npx hardhat test
```

### Backend Tests

```bash
cd backend

# Run all tests
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=AssetServiceTest

# View coverage report
open target/site/jacoco/index.html
```

### Integration Tests

```bash
# Start all services
docker-compose up -d

# Run integration tests (when implemented)
cd backend
mvn verify -P integration-tests
```

## 📊 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | User login | No |
| POST | `/api/auth/register` | User registration | No |
| GET | `/api/auth/me` | Get current user | Yes |

### Asset Management Endpoints

| Method | Endpoint | Description | Auth Required | Roles |
|--------|----------|-------------|---------------|-------|
| POST | `/api/assets/mint` | Mint new asset tokens | Yes | MINTER, ADMIN |
| POST | `/api/assets/burn` | Burn asset tokens | Yes | BURNER, ADMIN |
| GET | `/api/assets` | Get all assets | Yes | Any |
| GET | `/api/assets/{id}` | Get asset by ID | Yes | Any |
| GET | `/api/assets/type/{type}` | Get assets by type | Yes | Any |
| GET | `/api/assets/{id}/transactions` | Get transaction history | Yes | Any |
| GET | `/api/assets/{id}/balance/{address}` | Get balance | Yes | Any |
| GET | `/api/assets/holder/{address}` | Get holder assets | Yes | Any |

### Blockchain Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/blockchain/status` | Network status | No |
| GET | `/api/blockchain/transaction/{hash}` | Transaction details | Yes |

For detailed API documentation with request/response examples, visit the **Swagger UI** at:
http://localhost:8080/swagger-ui.html

## 🔧 Configuration

### Application Configuration (`application.yml`)

Key configuration properties:

```yaml
blockchain:
  besu:
    rpc-url: http://besu-node-1:8545
    ws-url: ws://besu-node-1:8546
    chain-id: 1337
  contract:
    asset-token-address: ${ASSET_TOKEN_CONTRACT_ADDRESS}

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000  # 24 hours

spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/tokenization
```

### Besu Network Configuration

- **Network ID**: 1337 (customizable)
- **Consensus**: IBFT 2.0
- **Block Time**: 2 seconds
- **Gas Limit**: 10,000,000
- **Validators**: 4 nodes

## 🐛 Troubleshooting

### Services Not Starting

```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f besu-node-1

# Restart services
docker-compose restart
```

### Contract Deployment Fails

```bash
# Ensure Besu network is running
curl -X POST http://localhost:8545 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}'

# Check deployer account has funds (pre-funded in genesis)
# Try deploying again with more gas
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose logs postgres

# Verify connection
docker-compose exec postgres psql -U tokenization_user -d tokenization -c "SELECT 1;"

# Reset database
docker-compose down -v
docker-compose up -d
```

### Backend Cannot Connect to Besu

```bash
# Check Besu RPC endpoint
curl http://localhost:8545

# Verify backend can reach Besu (from inside container)
docker-compose exec backend wget -qO- http://besu-node-1:8545

# Check environment variables
docker-compose exec backend env | grep BESU
```

## 📈 Monitoring and Logging

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f besu-node-1

# Backend application logs (inside container)
docker-compose exec backend tail -f /app/logs/application.log
```

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Besu network
curl -X POST http://localhost:8545 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}'

# Database
docker-compose exec postgres pg_isready
```

## 🚢 Deployment

### Development Environment

Already covered in Quick Start section above.

### Staging/Production

**Important Considerations:**

1. **Secrets Management**: Use proper secret management (HashiCorp Vault, AWS Secrets Manager)
2. **Database**: Use managed PostgreSQL (AWS RDS, Azure Database)
3. **Monitoring**: Set up Prometheus + Grafana
4. **Logging**: Centralized logging (ELK stack, Splunk)
5. **Backup**: Regular database and blockchain data backups
6. **SSL/TLS**: Enable HTTPS for all endpoints
7. **Firewall**: Restrict access to RPC endpoints
8. **Key Management**: Use hardware security modules (HSMs) for private keys

### Kubernetes Deployment (Future)

A Kubernetes deployment configuration can be added for production scalability.

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Standards

- **Java**: Follow Google Java Style Guide
- **Solidity**: Follow Solidity Style Guide
- **Tests**: Maintain 80%+ coverage for contracts, 70%+ for backend
- **Documentation**: Update docs for new features

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Enterprise Blockchain Team**
- Contact: your.email@example.com

## 🙏 Acknowledgments

- Hyperledger Besu Team
- OpenZeppelin for secure smart contract libraries
- Spring Boot and Web3j communities
- All contributors and testers

## 📚 Additional Resources

- [Hyperledger Besu Documentation](https://besu.hyperledger.org/)
- [Web3j Documentation](https://docs.web3j.io/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [OpenZeppelin Contracts](https://docs.openzeppelin.com/contracts/)
- [Hardhat Documentation](https://hardhat.org/docs)

## 🔮 Future Enhancements

- [ ] Frontend dashboard (React/Next.js)
- [ ] Advanced compliance features (KYC/AML)
- [ ] Multi-signature wallet support
- [ ] Asset lifecycle management
- [ ] Integration with external data oracles
- [ ] Advanced analytics and reporting
- [ ] Mobile application
- [ ] Kubernetes deployment configurations
- [ ] Support for other asset types (real estate, commodities)
- [ ] Cross-chain bridge integration

---

**Built with ❤️ for enterprise blockchain adoption**