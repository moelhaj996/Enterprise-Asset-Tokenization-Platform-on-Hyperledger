# System Architecture

## Overview

The Enterprise Asset Tokenization Platform is built on a multi-layered architecture that separates concerns between the blockchain layer, application layer, and data layer. This document provides detailed insights into the system design, component interactions, and data flows.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                             │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                 │
│  │  Web App   │  │  Mobile    │  │  External  │                 │
│  │            │  │  App       │  │  Systems   │                 │
│  └────────────┘  └────────────┘  └────────────┘                 │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS/REST
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Application Layer                           │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Spring Boot REST API                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │   │
│  │  │Controllers│  │ Services │  │ Security │               │   │
│  │  └──────────┘  └──────────┘  └──────────┘               │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │   │
│  │  │Blockchain│  │   Event   │  │   DTOs   │               │   │
│  │  │ Service  │  │ Listeners │  │          │               │   │
│  │  └──────────┘  └──────────┘  └──────────┘               │   │
│  └──────────────────────────────────────────────────────────┘   │
└────┬─────────────────────────────────────────────────┬──────────┘
     │                                                  │
     │ Web3j/JSON-RPC                                  │ JDBC
     ▼                                                  ▼
┌─────────────────────────────────────┐  ┌────────────────────────┐
│      Blockchain Layer               │  │     Data Layer         │
│  ┌───────────────────────────────┐  │  │  ┌──────────────────┐  │
│  │  Hyperledger Besu Network     │  │  │  │   PostgreSQL     │  │
│  │  ┌─────┐ ┌─────┐ ┌─────┐      │  │  │  │                  │  │
│  │  │Node1│ │Node2│ │Node3│ ...  │  │  │  │ - Users          │  │
│  │  └─────┘ └─────┘ └─────┘      │  │  │  │ - Assets         │  │
│  │         IBFT 2.0 Consensus     │  │  │  │ - Transactions   │  │
│  │                                 │  │  │  │ - Events         │  │
│  │  ┌──────────────────────────┐  │  │  │  │ - Asset Holders  │  │
│  │  │   Smart Contracts        │  │  │  │  └──────────────────┘  │
│  │  │  - AssetToken (ERC-20)   │  │  │  └────────────────────────┘
│  │  │  - AccessControl          │  │  │
│  │  └──────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

## Component Details

### 1. Blockchain Layer

#### Hyperledger Besu Network
- **Purpose**: Private, permissioned blockchain network
- **Consensus**: IBFT 2.0 (Istanbul Byzantine Fault Tolerance)
- **Nodes**: 4 validator nodes for Byzantine fault tolerance
- **Network ID**: 1337 (customizable)
- **Block Time**: 2 seconds
- **Gas Configuration**: Free gas (gas price = 0) for permissioned network

#### Smart Contracts

**AssetToken.sol**
- **Standard**: ERC-20 compatible
- **Features**:
  - Token minting with metadata
  - Token burning
  - Pause/unpause functionality
  - Role-based access control (ADMIN, MINTER, BURNER, PAUSER)
  - Asset metadata storage
  - Event emission for all operations

**Access Control**
- OpenZeppelin's AccessControl
- Granular role management
- Role-based function restrictions

### 2. Application Layer

#### Spring Boot Backend

**Web3j Integration**
- Connects to Besu via JSON-RPC
- Transaction management
- Event monitoring
- Contract interaction

**Core Services**

1. **BlockchainService**
   - Smart contract interaction
   - Transaction submission
   - Balance queries
   - Network status monitoring

2. **AssetService**
   - Asset minting workflow
   - Asset burning workflow
   - Asset queries
   - Transaction history

3. **AuthService**
   - User authentication
   - JWT token generation
   - User registration

4. **EventListenerService**
   - Real-time event monitoring
   - Database synchronization
   - Historical event replay
   - Connection recovery

**Security Layer**
- JWT authentication
- BCrypt password hashing
- Role-based authorization
- CORS configuration
- Input validation

**REST API**
- RESTful endpoints
- JSON request/response
- OpenAPI/Swagger documentation
- Consistent error handling

### 3. Data Layer

#### PostgreSQL Database

**Schema Design**

**users**
- User accounts
- Ethereum addresses
- Roles and permissions
- Authentication credentials

**assets**
- Asset registry
- Asset metadata
- Total supply tracking
- Contract addresses

**transactions**
- Transaction history
- Block information
- Gas usage
- Transaction status

**blockchain_events**
- Raw blockchain events
- Event processing status
- Event replay support

**asset_holders**
- Token holder registry
- Balance tracking
- Holder history

### 4. Client Layer

#### REST API Clients
- Web applications
- Mobile applications
- External systems integration
- Third-party integrations

## Data Flow Diagrams

### Asset Minting Flow

```
Client                API               AssetService        BlockchainService      Besu          Database
  │                    │                     │                     │                │              │
  │─POST /assets/mint─▶│                     │                     │                │              │
  │                    │──mintAsset()───────▶│                     │                │              │
  │                    │                     │──mintAsset()───────▶│                │              │
  │                    │                     │                     │─sendTransaction▶│              │
  │                    │                     │                     │                │─mine block──▶│
  │                    │                     │                     │◀───receipt──────│              │
  │                    │                     │◀───tx hash──────────│                │              │
  │                    │                     │──save Asset────────────────────────────────────────▶│
  │                    │                     │──save Transaction──────────────────────────────────▶│
  │                    │                     │──save AssetHolder──────────────────────────────────▶│
  │                    │◀──AssetResponse─────│                     │                │              │
  │◀──201 Created──────│                     │                     │                │              │
  │                    │                     │                     │                │              │
```

### Event Monitoring Flow

```
Besu          EventListenerService      Database
  │                    │                    │
  │─AssetMinted event─▶│                    │
  │                    │──save event───────▶│
  │                    │                    │
  │                    │──update asset─────▶│
  │                    │                    │
  │                    │──update holder────▶│
  │                    │                    │
  │─Transfer event────▶│                    │
  │                    │──save event───────▶│
  │                    │                    │
  │                    │──update balances──▶│
  │                    │                    │
```

### Authentication Flow

```
Client          AuthController      AuthService      UserRepository      JwtTokenProvider
  │                   │                  │                   │                   │
  │─POST /auth/login─▶│                  │                   │                   │
  │                   │──login()────────▶│                   │                   │
  │                   │                  │──findByUsername()─▶│                   │
  │                   │                  │◀─User entity──────│                   │
  │                   │                  │──verify password──│                   │
  │                   │                  │                   │                   │
  │                   │                  │──generateToken()─────────────────────▶│
  │                   │                  │◀────JWT token─────────────────────────│
  │                   │◀─AuthResponse────│                   │                   │
  │◀──200 OK (JWT)────│                  │                   │                   │
  │                   │                  │                   │                   │
```

## Security Architecture

### Network Security

1. **Private Network**
   - Permissioned blockchain
   - Known validators only
   - No public access to nodes

2. **API Security**
   - JWT authentication
   - Role-based authorization
   - HTTPS in production
   - CORS restrictions

3. **Data Security**
   - Encrypted database credentials
   - BCrypt password hashing
   - Private keys in environment variables
   - No sensitive data in logs

### Smart Contract Security

1. **Access Control**
   - Role-based permissions
   - Admin-only role management
   - Function-level restrictions

2. **Safety Mechanisms**
   - Emergency pause
   - Input validation
   - Zero-address checks
   - Reentrancy protection

3. **Audit Trail**
   - Event emission for all operations
   - Immutable transaction history
   - Off-chain event storage

## Scalability Considerations

### Horizontal Scaling

1. **Backend API**
   - Stateless design
   - Multiple API instances
   - Load balancer support

2. **Database**
   - Read replicas
   - Connection pooling
   - Optimized indexes

3. **Blockchain**
   - Additional validator nodes
   - Archive nodes for queries
   - Load-balanced RPC endpoints

### Vertical Scaling

1. **Besu Nodes**
   - Increased memory allocation
   - SSD storage
   - CPU optimization

2. **Database**
   - Increased connections
   - Larger buffer pool
   - Query optimization

## Monitoring and Observability

### Metrics to Monitor

1. **Blockchain Metrics**
   - Block production rate
   - Transaction throughput
   - Peer connections
   - Sync status

2. **Application Metrics**
   - API response times
   - Error rates
   - Database connection pool
   - Event processing lag

3. **System Metrics**
   - CPU usage
   - Memory usage
   - Disk I/O
   - Network bandwidth

### Logging Strategy

1. **Application Logs**
   - Structured logging (JSON)
   - Log levels (DEBUG, INFO, WARN, ERROR)
   - Request/response logging
   - Exception traces

2. **Blockchain Logs**
   - Node synchronization
   - Consensus events
   - Transaction execution
   - Smart contract calls

3. **Audit Logs**
   - User authentication
   - Role changes
   - Asset operations
   - Configuration changes

## Disaster Recovery

### Backup Strategy

1. **Database Backups**
   - Daily full backups
   - Continuous transaction logs
   - Point-in-time recovery

2. **Blockchain Data**
   - Periodic snapshots
   - Full node backups
   - Genesis file preservation

3. **Configuration**
   - Version-controlled configs
   - Secret management backups
   - Infrastructure as Code

### Recovery Procedures

1. **Database Failure**
   - Restore from latest backup
   - Replay transaction logs
   - Verify data integrity

2. **Blockchain Failure**
   - Restore from snapshot
   - Re-sync from other nodes
   - Replay events to database

3. **Application Failure**
   - Redeploy from CI/CD
   - Health check verification
   - Gradual traffic restoration

## Technology Choices Rationale

### Why Hyperledger Besu?
- Enterprise-ready private blockchain
- IBFT 2.0 for fast finality
- EVM compatibility
- Active community support

### Why Java Spring Boot?
- Enterprise standard
- Excellent Web3j integration
- Rich ecosystem
- Production-proven

### Why PostgreSQL?
- ACID compliance
- JSON support (JSONB)
- Strong consistency
- Battle-tested reliability

### Why Web3j?
- Native Java integration
- Type-safe contracts
- Active development
- Comprehensive documentation

## Future Architecture Enhancements

1. **Microservices**
   - Split into domain services
   - Event-driven architecture
   - Service mesh (Istio)

2. **Caching Layer**
   - Redis for frequently accessed data
   - Blockchain query caching
   - Session management

3. **Message Queue**
   - RabbitMQ/Kafka for async processing
   - Event sourcing
   - CQRS pattern

4. **Multi-Region Deployment**
   - Geographic distribution
   - Data replication
   - Disaster recovery

5. **Advanced Monitoring**
   - Prometheus metrics
   - Grafana dashboards
   - Distributed tracing (Jaeger)

---

For implementation details, see the codebase documentation and README.md.