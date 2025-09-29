# Deployment Guide

This guide provides step-by-step instructions for deploying the Enterprise Asset Tokenization Platform in various environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Deployment](#local-development-deployment)
3. [Production Deployment Checklist](#production-deployment-checklist)
4. [Environment Configuration](#environment-configuration)
5. [Database Setup](#database-setup)
6. [Blockchain Network Setup](#blockchain-network-setup)
7. [Backend Deployment](#backend-deployment)
8. [Monitoring and Maintenance](#monitoring-and-maintenance)
9. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Docker | 20.10+ | Containerization |
| Docker Compose | 2.0+ | Multi-container orchestration |
| Java (JDK) | 17+ | Backend development |
| Node.js | 18+ | Smart contract development |
| Git | 2.30+ | Version control |
| Maven | 3.8+ | Java build tool |

### System Requirements

**Minimum (Development):**
- CPU: 4 cores
- RAM: 8 GB
- Disk: 50 GB SSD
- Network: 10 Mbps

**Recommended (Production):**
- CPU: 8 cores
- RAM: 16 GB
- Disk: 200 GB SSD
- Network: 100 Mbps

## Local Development Deployment

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd Enterprise-Asset-Tokenization-Platform-on-Hyperledger
```

### Step 2: Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit environment variables
nano .env
```

**Critical Variables to Update:**
```env
# Security
JWT_SECRET=<generate-a-strong-256-bit-secret>
DB_PASSWORD=<set-secure-database-password>

# Blockchain
DEPLOYER_PRIVATE_KEY=<your-private-key>

# Network
BESU_RPC_URL=http://besu-node-1:8545
```

**Generate Strong JWT Secret:**
```bash
# Using OpenSSL
openssl rand -base64 64

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(64))"
```

### Step 3: Install Dependencies

**Smart Contracts:**
```bash
cd contracts
npm install
cd ..
```

**Backend (Optional for local dev):**
```bash
cd backend
mvn clean install -DskipTests
cd ..
```

### Step 4: Start Services

```bash
# Start all services in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

**Expected Output:**
```
NAME                COMMAND                  SERVICE             STATUS
besu-node-1         "besu --genesis-file…"   besu-node-1         healthy
besu-node-2         "besu --genesis-file…"   besu-node-2         running
besu-node-3         "besu --genesis-file…"   besu-node-3         running
besu-node-4         "besu --genesis-file…"   besu-node-4         running
postgres            "docker-entrypoint.s…"   postgres            healthy
backend             "sh -c 'java $JAVA_O…"   backend             healthy
```

### Step 5: Deploy Smart Contracts

```bash
cd contracts

# Compile contracts
npx hardhat compile

# Deploy to Besu network
npx hardhat run scripts/deploy.js --network besu
```

**Expected Output:**
```
Deploying AssetToken contract...
Deploying with account: 0xfe3b557e8fb62b89f4916b721be55ceb828dbd73
Account balance: 90000.0 ETH
AssetToken deployed to: 0x5FbDB2315678afecb367f032d93F642f64180aa3

=== Update your .env file with: ===
ASSET_TOKEN_CONTRACT_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
```

### Step 6: Update Contract Address

```bash
# Update .env file with deployed contract address
nano .env
# Add: ASSET_TOKEN_CONTRACT_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3

# Restart backend to load new contract address
docker-compose restart backend
```

### Step 7: Verify Deployment

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check blockchain status
curl http://localhost:8080/api/blockchain/status

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Production Deployment Checklist

### Security Checklist

- [ ] **Generate production JWT secret** (not from .env.example)
- [ ] **Use strong database password** (min 16 characters, random)
- [ ] **Store private keys securely** (AWS Secrets Manager, HashiCorp Vault)
- [ ] **Enable HTTPS/TLS** for all API endpoints
- [ ] **Configure firewall rules** (restrict RPC access)
- [ ] **Enable database encryption** at rest and in transit
- [ ] **Set up VPN** for internal communication
- [ ] **Implement rate limiting** on API endpoints
- [ ] **Enable CORS** only for trusted domains
- [ ] **Use non-root users** in all containers
- [ ] **Regular security updates** for all dependencies
- [ ] **Enable audit logging** for all operations

### Infrastructure Checklist

- [ ] **Set up load balancer** (Nginx, HAProxy, AWS ALB)
- [ ] **Configure auto-scaling** for backend API
- [ ] **Set up database replication** (primary-replica)
- [ ] **Configure backup strategy** (daily full, hourly incremental)
- [ ] **Set up monitoring** (Prometheus + Grafana)
- [ ] **Configure logging** (ELK stack or CloudWatch)
- [ ] **Set up alerting** (PagerDuty, Opsgenie)
- [ ] **Configure CDN** for static assets (if applicable)
- [ ] **Set up DNS** with failover
- [ ] **Configure SSL certificates** (Let's Encrypt, commercial CA)

### Application Checklist

- [ ] **Run all tests** (unit, integration, e2e)
- [ ] **Security audit** of smart contracts (Slither, MythX)
- [ ] **Performance testing** (load testing, stress testing)
- [ ] **Database migrations** tested and verified
- [ ] **Environment variables** properly configured
- [ ] **Logging levels** set appropriately (INFO in production)
- [ ] **Error handling** reviewed and tested
- [ ] **API documentation** up to date
- [ ] **Deployment runbook** created
- [ ] **Rollback plan** documented

## Environment Configuration

### Development (.env.development)

```env
SPRING_PROFILES_ACTIVE=dev
LOGGING_LEVEL_COM_ENTERPRISE_TOKENIZATION=DEBUG
BESU_RPC_URL=http://localhost:8545
JWT_EXPIRATION_MS=86400000
```

### Staging (.env.staging)

```env
SPRING_PROFILES_ACTIVE=staging
LOGGING_LEVEL_COM_ENTERPRISE_TOKENIZATION=INFO
BESU_RPC_URL=http://besu-staging.internal:8545
JWT_EXPIRATION_MS=43200000
```

### Production (.env.production)

```env
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_COM_ENTERPRISE_TOKENIZATION=WARN
BESU_RPC_URL=http://besu-prod.internal:8545
JWT_EXPIRATION_MS=3600000
```

## Database Setup

### PostgreSQL Configuration

**For Development:**
```bash
docker-compose up -d postgres
```

**For Production:**
1. Use managed database service (AWS RDS, Azure Database, Google Cloud SQL)
2. Enable automated backups
3. Set up read replicas
4. Configure connection pooling
5. Enable SSL/TLS connections

**Connection String Format:**
```
jdbc:postgresql://<host>:<port>/<database>?ssl=true&sslmode=require
```

### Database Migrations

Migrations run automatically on startup via Flyway.

**Manual Migration:**
```bash
cd backend
mvn flyway:migrate
```

**Rollback (if needed):**
```bash
mvn flyway:undo
```

### Database Backup

**Automated Daily Backup:**
```bash
# Create backup script
cat > backup-db.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/tokenization_$TIMESTAMP.sql"

docker-compose exec -T postgres pg_dump -U tokenization_user tokenization > $BACKUP_FILE
gzip $BACKUP_FILE

# Delete backups older than 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
EOF

chmod +x backup-db.sh

# Add to crontab for daily execution
crontab -e
# Add: 0 2 * * * /path/to/backup-db.sh
```

## Blockchain Network Setup

### Besu Node Configuration

**Single-Region Deployment:**
- 4 validator nodes (minimum for IBFT 2.0)
- 1-2 RPC nodes (for API access)
- 1 archive node (for historical queries)

**Multi-Region Deployment:**
- 2 validators per region (3 regions = 6 validators)
- Regional RPC nodes
- Cross-region connectivity

### Genesis File Configuration

**Key Parameters:**
```json
{
  "config": {
    "chainId": 1337,
    "ibft2": {
      "blockperiodseconds": 2,
      "epochlength": 30000,
      "requesttimeoutseconds": 4
    }
  },
  "gasLimit": "0x989680"
}
```

### Node Startup

**Validator Node:**
```bash
docker run -d --name besu-validator-1 \
  -v ./config/besu:/config \
  -v besu-data-1:/data \
  -p 8545:8545 \
  -p 30303:30303 \
  hyperledger/besu:latest \
  --genesis-file=/config/genesis.json \
  --network-id=1337 \
  --rpc-http-enabled \
  --rpc-http-host=0.0.0.0 \
  --p2p-enabled=true \
  --miner-enabled
```

## Backend Deployment

### Build Application

```bash
cd backend
mvn clean package -DskipTests
```

### Docker Deployment

**Build Image:**
```bash
docker build -t tokenization-backend:1.0.0 .
```

**Run Container:**
```bash
docker run -d \
  --name backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tokenization \
  -e JWT_SECRET=$JWT_SECRET \
  -e BESU_RPC_URL=$BESU_RPC_URL \
  tokenization-backend:1.0.0
```

### Kubernetes Deployment (Optional)

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tokenization-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: tokenization-backend
  template:
    metadata:
      labels:
        app: tokenization-backend
    spec:
      containers:
      - name: backend
        image: tokenization-backend:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## Monitoring and Maintenance

### Health Checks

**Backend:**
```bash
curl http://localhost:8080/actuator/health
```

**Blockchain:**
```bash
curl -X POST http://localhost:8545 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_syncing","params":[],"id":1}'
```

**Database:**
```bash
docker-compose exec postgres pg_isready
```

### Log Monitoring

```bash
# Backend logs
docker-compose logs -f backend

# Besu logs
docker-compose logs -f besu-node-1

# PostgreSQL logs
docker-compose logs -f postgres

# Export logs for analysis
docker-compose logs --no-color > system-logs-$(date +%Y%m%d).log
```

### Performance Monitoring

**Key Metrics:**
- API response times
- Database connection pool usage
- Blockchain sync status
- Transaction throughput
- Error rates
- Memory usage
- CPU usage

**Tools:**
- Prometheus for metrics collection
- Grafana for visualization
- Jaeger for distributed tracing
- ELK stack for log aggregation

## Troubleshooting

### Common Issues

**1. Backend Cannot Connect to Besu**

```bash
# Check Besu is running
docker-compose ps besu-node-1

# Check Besu RPC endpoint
curl http://localhost:8545

# Verify network connectivity
docker-compose exec backend ping besu-node-1
```

**2. Database Connection Failed**

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test connection
docker-compose exec postgres psql -U tokenization_user -d tokenization -c "SELECT 1;"

# Check credentials in .env
cat .env | grep DB_
```

**3. Smart Contract Deployment Failed**

```bash
# Check account has funds
npx hardhat console --network besu
> const balance = await ethers.provider.getBalance("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73")
> console.log(ethers.formatEther(balance))

# Increase gas limit
# Edit hardhat.config.js and increase gas values
```

**4. Event Listener Not Working**

```bash
# Check contract address is set
docker-compose exec backend env | grep CONTRACT

# Check WebSocket connection
curl http://localhost:8546

# Restart backend
docker-compose restart backend
```

### Emergency Procedures

**System Restart:**
```bash
docker-compose down
docker-compose up -d
```

**Database Recovery:**
```bash
# Stop services
docker-compose down

# Restore from backup
gunzip < /backups/tokenization_YYYYMMDD_HHMMSS.sql.gz | \
  docker-compose exec -T postgres psql -U tokenization_user tokenization

# Start services
docker-compose up -d
```

**Rollback Deployment:**
```bash
# Pull previous version
git checkout <previous-tag>

# Rebuild and deploy
docker-compose build
docker-compose up -d
```

## Support

For deployment support:
- Email: devops@example.com
- Slack: #deployment-support
- On-call: PagerDuty

---

**Last Updated:** 2024-09-29