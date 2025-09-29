# API Documentation

## Base URL

```
http://localhost:8080/api
```

For production, replace with your domain and ensure HTTPS is enabled.

## Authentication

Most endpoints require JWT authentication. To authenticate:

1. **Login** using `/api/auth/login`
2. Use the returned JWT token in the `Authorization` header:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

## Response Format

### Success Response

```json
{
  "data": { ... },
  "timestamp": "2024-09-29T10:30:00Z"
}
```

### Error Response

```json
{
  "timestamp": "2024-09-29T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: amount must be positive",
  "path": "/api/assets/mint"
}
```

## HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request succeeded |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Authentication required or failed |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource already exists |
| 500 | Internal Server Error | Server error occurred |
| 503 | Service Unavailable | Service temporarily unavailable |

---

## Authentication Endpoints

### 1. Login

Authenticate user and receive JWT token.

**Endpoint:** `POST /api/auth/login`

**Authentication:** Not required

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "admin",
  "role": "ADMIN"
}
```

**Error Responses:**
- `400 Bad Request` - Missing or invalid parameters
- `401 Unauthorized` - Invalid credentials

---

### 2. Register

Register a new user account.

**Endpoint:** `POST /api/auth/register`

**Authentication:** Not required

**Request Body:**
```json
{
  "username": "newuser",
  "password": "securePassword123",
  "ethereumAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "role": "INVESTOR"
}
```

**Validation Rules:**
- `username`: 3-50 characters, unique
- `password`: Minimum 8 characters
- `ethereumAddress`: Valid Ethereum address (0x + 40 hex chars), unique
- `role`: One of: ADMIN, ISSUER, INVESTOR, AUDITOR, USER

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "username": "newuser",
  "role": "INVESTOR"
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `409 Conflict` - Username or Ethereum address already exists

---

### 3. Get Current User

Get details of the authenticated user.

**Endpoint:** `GET /api/auth/me`

**Authentication:** Required

**Response:** `200 OK`
```json
{
  "id": 1,
  "username": "admin",
  "ethereumAddress": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
  "role": "ADMIN",
  "enabled": true,
  "createdAt": "2024-09-29T10:00:00Z",
  "updatedAt": "2024-09-29T10:00:00Z"
}
```

**Error Responses:**
- `401 Unauthorized` - No valid JWT token

---

## Asset Management Endpoints

### 4. Mint Asset

Create new asset tokens on the blockchain.

**Endpoint:** `POST /api/assets/mint`

**Authentication:** Required

**Authorization:** MINTER or ADMIN role

**Request Body:**
```json
{
  "assetId": "BOND-2024-001",
  "assetType": "CORPORATE_BOND",
  "amount": 1000000,
  "recipient": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "issuer": "Acme Corporation",
  "metadata": {
    "maturityDate": "2029-12-31",
    "couponRate": 5.5,
    "faceValue": 1000,
    "currency": "USD"
  }
}
```

**Validation Rules:**
- `assetId`: Unique identifier, not blank
- `assetType`: Not blank (e.g., CORPORATE_BOND, INVOICE, SUPPLY_CHAIN_ASSET)
- `amount`: Positive BigInteger
- `recipient`: Valid Ethereum address
- `issuer`: Not blank
- `metadata`: Optional key-value pairs

**Response:** `201 Created`
```json
{
  "id": 1,
  "assetId": "BOND-2024-001",
  "assetType": "CORPORATE_BOND",
  "totalSupply": 1000000,
  "issuer": "Acme Corporation",
  "status": "ACTIVE",
  "metadata": {
    "maturityDate": "2029-12-31",
    "couponRate": 5.5,
    "faceValue": 1000,
    "currency": "USD"
  },
  "contractAddress": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
  "createdAt": "2024-09-29T10:30:00Z",
  "updatedAt": "2024-09-29T10:30:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Insufficient permissions
- `409 Conflict` - Asset ID already exists
- `500 Internal Server Error` - Blockchain transaction failed

---

### 5. Burn Asset

Destroy (burn) asset tokens from the blockchain.

**Endpoint:** `POST /api/assets/burn`

**Authentication:** Required

**Authorization:** BURNER or ADMIN role

**Request Body:**
```json
{
  "assetId": "BOND-2024-001",
  "amount": 100000,
  "from": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb"
}
```

**Validation Rules:**
- `assetId`: Must exist
- `amount`: Positive, not exceeding holder's balance
- `from`: Valid Ethereum address with sufficient balance

**Response:** `200 OK`
```json
{
  "id": 42,
  "transactionHash": "0xabc123...",
  "transactionType": "BURN",
  "assetId": "BOND-2024-001",
  "fromAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "toAddress": "0x0000000000000000000000000000000000000000",
  "amount": 100000,
  "status": "SUCCESS",
  "blockNumber": 12345,
  "blockHash": "0xdef456...",
  "gasUsed": 52000,
  "timestamp": "2024-09-29T10:35:00Z",
  "createdAt": "2024-09-29T10:35:05Z"
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed or insufficient balance
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Asset not found
- `500 Internal Server Error` - Blockchain transaction failed

---

### 6. Get All Assets

Retrieve all assets in the system.

**Endpoint:** `GET /api/assets`

**Authentication:** Required

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "assetId": "BOND-2024-001",
    "assetType": "CORPORATE_BOND",
    "totalSupply": 900000,
    "issuer": "Acme Corporation",
    "status": "ACTIVE",
    "metadata": {...},
    "contractAddress": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
    "createdAt": "2024-09-29T10:30:00Z",
    "updatedAt": "2024-09-29T10:35:00Z"
  },
  {
    "id": 2,
    "assetId": "INVOICE-2024-042",
    "assetType": "INVOICE",
    "totalSupply": 50000,
    "issuer": "Beta LLC",
    "status": "ACTIVE",
    "metadata": {...},
    "contractAddress": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
    "createdAt": "2024-09-29T11:00:00Z",
    "updatedAt": "2024-09-29T11:00:00Z"
  }
]
```

---

### 7. Get Asset by ID

Retrieve a specific asset by its ID.

**Endpoint:** `GET /api/assets/{assetId}`

**Authentication:** Required

**Path Parameters:**
- `assetId` - Unique asset identifier

**Response:** `200 OK`
```json
{
  "id": 1,
  "assetId": "BOND-2024-001",
  "assetType": "CORPORATE_BOND",
  "totalSupply": 900000,
  "issuer": "Acme Corporation",
  "status": "ACTIVE",
  "metadata": {
    "maturityDate": "2029-12-31",
    "couponRate": 5.5,
    "faceValue": 1000,
    "currency": "USD"
  },
  "contractAddress": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
  "createdAt": "2024-09-29T10:30:00Z",
  "updatedAt": "2024-09-29T10:35:00Z"
}
```

**Error Responses:**
- `401 Unauthorized` - Not authenticated
- `404 Not Found` - Asset not found

---

### 8. Get Assets by Type

Retrieve all assets of a specific type.

**Endpoint:** `GET /api/assets/type/{type}`

**Authentication:** Required

**Path Parameters:**
- `type` - Asset type (e.g., CORPORATE_BOND, INVOICE)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "assetId": "BOND-2024-001",
    "assetType": "CORPORATE_BOND",
    ...
  }
]
```

---

### 9. Get Asset Transaction History

Retrieve all transactions for a specific asset.

**Endpoint:** `GET /api/assets/{assetId}/transactions`

**Authentication:** Required

**Path Parameters:**
- `assetId` - Unique asset identifier

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "transactionHash": "0xabc123...",
    "transactionType": "MINT",
    "assetId": "BOND-2024-001",
    "fromAddress": "0x0000000000000000000000000000000000000000",
    "toAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
    "amount": 1000000,
    "status": "SUCCESS",
    "blockNumber": 12340,
    "timestamp": "2024-09-29T10:30:00Z"
  },
  {
    "id": 42,
    "transactionHash": "0xdef456...",
    "transactionType": "BURN",
    "assetId": "BOND-2024-001",
    "fromAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
    "toAddress": "0x0000000000000000000000000000000000000000",
    "amount": 100000,
    "status": "SUCCESS",
    "blockNumber": 12345,
    "timestamp": "2024-09-29T10:35:00Z"
  }
]
```

---

### 10. Get Asset Balance for Address

Get the balance of a specific asset for an Ethereum address.

**Endpoint:** `GET /api/assets/{assetId}/balance/{address}`

**Authentication:** Required

**Path Parameters:**
- `assetId` - Unique asset identifier
- `address` - Ethereum address

**Response:** `200 OK`
```json
900000
```

**Error Responses:**
- `400 Bad Request` - Invalid address format
- `401 Unauthorized` - Not authenticated
- `404 Not Found` - Asset not found

---

### 11. Get Assets Held by Address

Get all assets held by a specific Ethereum address.

**Endpoint:** `GET /api/assets/holder/{address}`

**Authentication:** Required

**Path Parameters:**
- `address` - Ethereum address

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "assetId": "BOND-2024-001",
    "assetType": "CORPORATE_BOND",
    "totalSupply": 900000,
    ...
  },
  {
    "id": 3,
    "assetId": "INVOICE-2024-055",
    "assetType": "INVOICE",
    "totalSupply": 25000,
    ...
  }
]
```

---

## Blockchain Endpoints

### 12. Get Blockchain Status

Get the current status of the blockchain network.

**Endpoint:** `GET /api/blockchain/status`

**Authentication:** Not required (public endpoint)

**Response:** `200 OK`
```json
{
  "chainId": 1337,
  "blockNumber": 12350,
  "peerCount": 3,
  "syncing": false
}
```

**Error Responses:**
- `503 Service Unavailable` - Cannot connect to blockchain

---

### 13. Get Transaction Details

Get detailed information about a blockchain transaction.

**Endpoint:** `GET /api/blockchain/transaction/{hash}`

**Authentication:** Required

**Path Parameters:**
- `hash` - Transaction hash (0x + 64 hex characters)

**Response:** `200 OK`
```json
{
  "transactionHash": "0xabc123...",
  "blockNumber": 12345,
  "blockHash": "0xdef456...",
  "from": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
  "to": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
  "gasUsed": 52000,
  "status": "SUCCESS",
  "logs": [...]
}
```

**Error Responses:**
- `400 Bad Request` - Invalid transaction hash format
- `401 Unauthorized` - Not authenticated
- `404 Not Found` - Transaction not found

---

## Rate Limiting

Currently, no rate limiting is implemented. For production, consider implementing:
- 100 requests per minute per IP for public endpoints
- 1000 requests per minute per authenticated user

## Pagination

For endpoints returning lists, pagination will be added in future versions:
```
GET /api/assets?page=0&size=20&sort=createdAt,desc
```

## Versioning

API versioning will be added in future releases:
```
GET /api/v1/assets
GET /api/v2/assets
```

## Webhooks (Future)

Register webhooks to receive real-time notifications:
- Asset minted
- Asset burned
- Transfer completed
- Balance threshold reached

---

## Interactive API Documentation

For interactive API testing, visit:
**http://localhost:8080/swagger-ui.html**

The Swagger UI provides:
- Complete API reference
- Request/response examples
- Interactive testing
- Authentication support
- Schema definitions

---

## Support

For API support or questions:
- Email: api-support@example.com
- Documentation: https://docs.example.com
- GitHub Issues: https://github.com/yourorg/repo/issues