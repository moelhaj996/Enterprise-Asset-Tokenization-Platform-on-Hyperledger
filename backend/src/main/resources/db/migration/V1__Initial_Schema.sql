-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    ethereum_address VARCHAR(42) UNIQUE,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_ethereum_address ON users(ethereum_address);

-- Assets table
CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,
    asset_id VARCHAR(255) UNIQUE NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    total_supply NUMERIC(38, 0) NOT NULL,
    contract_address VARCHAR(42) NOT NULL,
    issuer VARCHAR(255),
    issuance_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX idx_assets_asset_id ON assets(asset_id);
CREATE INDEX idx_assets_asset_type ON assets(asset_type);
CREATE INDEX idx_assets_contract_address ON assets(contract_address);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_hash VARCHAR(66) UNIQUE NOT NULL,
    asset_id VARCHAR(255),
    from_address VARCHAR(42),
    to_address VARCHAR(42),
    amount NUMERIC(38, 0),
    transaction_type VARCHAR(20) NOT NULL,
    block_number BIGINT NOT NULL,
    block_timestamp TIMESTAMP,
    gas_used BIGINT,
    gas_price BIGINT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES assets(asset_id) ON DELETE SET NULL
);

CREATE INDEX idx_transactions_hash ON transactions(transaction_hash);
CREATE INDEX idx_transactions_asset_id ON transactions(asset_id);
CREATE INDEX idx_transactions_from_address ON transactions(from_address);
CREATE INDEX idx_transactions_to_address ON transactions(to_address);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_block_number ON transactions(block_number);
CREATE INDEX idx_transactions_status ON transactions(status);

-- Blockchain events table
CREATE TABLE blockchain_events (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(100) NOT NULL,
    contract_address VARCHAR(42) NOT NULL,
    block_number BIGINT NOT NULL,
    transaction_hash VARCHAR(66) NOT NULL,
    log_index INT NOT NULL,
    event_data JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(transaction_hash, log_index)
);

CREATE INDEX idx_events_name ON blockchain_events(event_name);
CREATE INDEX idx_events_contract ON blockchain_events(contract_address);
CREATE INDEX idx_events_block ON blockchain_events(block_number);
CREATE INDEX idx_events_tx_hash ON blockchain_events(transaction_hash);
CREATE INDEX idx_events_processed ON blockchain_events(processed);

-- Asset holders table (tracks who holds which assets)
CREATE TABLE asset_holders (
    id BIGSERIAL PRIMARY KEY,
    asset_id VARCHAR(255) NOT NULL,
    holder_address VARCHAR(42) NOT NULL,
    balance NUMERIC(38, 0) NOT NULL DEFAULT 0,
    first_acquired TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES assets(asset_id) ON DELETE CASCADE,
    UNIQUE(asset_id, holder_address)
);

CREATE INDEX idx_holders_asset_id ON asset_holders(asset_id);
CREATE INDEX idx_holders_address ON asset_holders(holder_address);

-- Insert default admin user (password: admin123 - BCrypt hash)
INSERT INTO users (username, password_hash, ethereum_address, role)
VALUES (
    'admin',
    '$2a$10$ZqKh9yPD8R7n0L0iVXxZ4.oXzKVfEf5V0iZs8GqZ8Rp6zXqZ8Rp6z',
    '0xfe3b557e8fb62b89f4916b721be55ceb828dbd73',
    'ADMIN'
);

-- Insert test users
INSERT INTO users (username, password_hash, ethereum_address, role)
VALUES (
    'minter',
    '$2a$10$ZqKh9yPD8R7n0L0iVXxZ4.oXzKVfEf5V0iZs8GqZ8Rp6zXqZ8Rp6z',
    '0x627306090abaB3A6e1400e9345bC60c78a8BEf57',
    'MINTER'
);

INSERT INTO users (username, password_hash, ethereum_address, role)
VALUES (
    'burner',
    '$2a$10$ZqKh9yPD8R7n0L0iVXxZ4.oXzKVfEf5V0iZs8GqZ8Rp6zXqZ8Rp6z',
    '0xf17f52151EbEF6C7334FAD080c5704D77216b732',
    'BURNER'
);