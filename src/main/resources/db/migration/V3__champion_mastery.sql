-- Champion Mastery table
-- Stores mastery points per user per champion (token)

CREATE TABLE champion_mastery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_index INTEGER NOT NULL,
    mastery_points BIGINT NOT NULL DEFAULT 0,
    games_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    podium_finishes INTEGER NOT NULL DEFAULT 0,
    best_placement INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, asset_index)
);

-- Indexes for efficient queries
CREATE INDEX idx_champion_mastery_user_id ON champion_mastery(user_id);
CREATE INDEX idx_champion_mastery_asset_index ON champion_mastery(asset_index);
CREATE INDEX idx_champion_mastery_mastery_points ON champion_mastery(mastery_points DESC);

-- Comments
COMMENT ON TABLE champion_mastery IS 'Stores user mastery progression for each champion (token)';
COMMENT ON COLUMN champion_mastery.asset_index IS 'Token index (0-13) matching the Solana program';
COMMENT ON COLUMN champion_mastery.mastery_points IS 'Total mastery points earned with this champion';
COMMENT ON COLUMN champion_mastery.games_played IS 'Number of arena games played with this champion';
COMMENT ON COLUMN champion_mastery.wins IS 'Number of 1st place finishes';
COMMENT ON COLUMN champion_mastery.podium_finishes IS 'Number of top 3 finishes';
COMMENT ON COLUMN champion_mastery.best_placement IS 'Best placement achieved (1-10)';

