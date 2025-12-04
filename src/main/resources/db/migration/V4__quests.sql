-- Quests table - defines available quests
CREATE TABLE quests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    quest_type VARCHAR(20) NOT NULL, -- ONE_TIME or WEEKLY
    gold_reward INTEGER NOT NULL DEFAULT 0,
    required_amount INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER DEFAULT 0
);

-- User quest progress table
CREATE TABLE user_quest_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id UUID NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    current_amount INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    reward_claimed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP,
    reward_claimed_at TIMESTAMP,
    week_key VARCHAR(10), -- For weekly quests: YYYY-WW format
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, quest_id, week_key)
);

-- Indexes
CREATE INDEX idx_user_quest_progress_user_id ON user_quest_progress(user_id);
CREATE INDEX idx_user_quest_progress_quest_id ON user_quest_progress(quest_id);
CREATE INDEX idx_user_quest_progress_week_key ON user_quest_progress(week_key);

-- Insert default quests

-- One-time quest: Link a Solana wallet
INSERT INTO quests (code, title, description, quest_type, gold_reward, required_amount, display_order)
VALUES ('LINK_WALLET', 'Link a Solana wallet', 'Connect your Solana wallet to your account', 'ONE_TIME', 50, 1, 1);

-- Weekly quest: Enter 3 different arenas
INSERT INTO quests (code, title, description, quest_type, gold_reward, required_amount, display_order)
VALUES ('ENTER_3_ARENAS', 'Enter 3 different arenas', 'Participate in 3 arena battles this week', 'WEEKLY', 30, 3, 2);

-- Weekly quest: Win one Arena match
INSERT INTO quests (code, title, description, quest_type, gold_reward, required_amount, display_order)
VALUES ('WIN_ARENA', 'Win one Arena match', 'Be victorious in an arena battle', 'WEEKLY', 100, 1, 3);

-- Comments
COMMENT ON TABLE quests IS 'Defines available quests in the system';
COMMENT ON TABLE user_quest_progress IS 'Tracks user progress on quests';
COMMENT ON COLUMN user_quest_progress.week_key IS 'For weekly quests, identifies the week (YYYY-WW format)';

