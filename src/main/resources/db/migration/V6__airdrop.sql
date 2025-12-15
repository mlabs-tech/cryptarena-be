-- Create airdrop table to track SOL testnet faucet claims
CREATE TABLE airdrops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_address VARCHAR(255) NOT NULL,
    amount_sol DECIMAL(10, 6) NOT NULL,
    transaction_signature VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_airdrops_user_id ON airdrops(user_id);
CREATE INDEX idx_airdrops_wallet_address ON airdrops(wallet_address);
CREATE INDEX idx_airdrops_claimed_at ON airdrops(claimed_at);
CREATE INDEX idx_airdrops_status ON airdrops(status);

-- Composite index for cooldown checks
CREATE INDEX idx_airdrops_wallet_claimed ON airdrops(wallet_address, claimed_at);

