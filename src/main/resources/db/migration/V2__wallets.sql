-- Create wallets table for Web3 wallet linking
CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    address VARCHAR(255) NOT NULL UNIQUE,
    wallet_type VARCHAR(50) NOT NULL DEFAULT 'SOLANA',
    label VARCHAR(100),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient querying
CREATE INDEX idx_wallets_user_id ON wallets(user_id);
CREATE INDEX idx_wallets_address ON wallets(address);
CREATE INDEX idx_wallets_wallet_type ON wallets(wallet_type);

-- Ensure only one primary wallet per user per wallet type
CREATE UNIQUE INDEX idx_wallets_user_primary ON wallets(user_id, wallet_type) WHERE is_primary = TRUE;

