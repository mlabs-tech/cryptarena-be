-- Initial migration - Cryptarena Database Setup

-- Create crypto_coins table
CREATE TABLE crypto_coins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(50) NOT NULL UNIQUE,
    current_price NUMERIC(30, 10),
    market_cap NUMERIC(30, 2)
);

CREATE INDEX idx_crypto_coins_symbol ON crypto_coins(symbol);

-- Create champions table
CREATE TABLE champions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    profile_picture VARCHAR(500),
    profile_banner VARCHAR(500),
    profile_character VARCHAR(500),
    crypto_coin_id UUID UNIQUE,
    CONSTRAINT fk_champions_crypto_coin FOREIGN KEY (crypto_coin_id) REFERENCES crypto_coins(id)
);

CREATE INDEX idx_champions_name ON champions(name);
CREATE INDEX idx_champions_crypto_coin_id ON champions(crypto_coin_id);

-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    twitter_name VARCHAR(255),
    twitter_id VARCHAR(255) UNIQUE,
    twitter_profile_picture VARCHAR(500),
    twitter_username VARCHAR(255) UNIQUE,
    gold NUMERIC(30, 2) DEFAULT 0,
    profile_banner VARCHAR(500)
);

CREATE INDEX idx_users_twitter_id ON users(twitter_id);
CREATE INDEX idx_users_twitter_username ON users(twitter_username);
