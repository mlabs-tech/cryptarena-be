package com.cryptarena.backend.service;

import com.cryptarena.backend.config.CoinMarketCapConfig;
import com.cryptarena.backend.dto.crypto.CryptoCoinDto;
import com.cryptarena.backend.exception.AuthenticationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoCoinService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CoinMarketCapService coinMarketCapService;
    private final CoinMarketCapConfig coinMarketCapConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_KEY_PREFIX = "crypto:coin:";

    // Supported tokens
    private static final Set<String> SUPPORTED_SYMBOLS = Set.of(
            "SOL", "TRUMP", "PUMP", "BONK", "JUP", "PENGU", "PYTH",
            "HNT", "FARTCOIN", "RAY", "JTO", "KMNO", "MET", "W"
    );

    /**
     * Get cryptocurrency data by symbol with Redis caching
     */
    public CryptoCoinDto getCoinBySymbol(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        
        // Validate symbol is supported
        if (!SUPPORTED_SYMBOLS.contains(upperSymbol)) {
            throw new AuthenticationException("Unsupported cryptocurrency symbol: " + symbol);
        }

        String cacheKey = CACHE_KEY_PREFIX + upperSymbol;

        // Try to get from cache first
        CryptoCoinDto cachedCoin = getFromCache(cacheKey);
        if (cachedCoin != null) {
            log.debug("Cache hit for symbol: {}", upperSymbol);
            return cachedCoin;
        }

        log.debug("Cache miss for symbol: {}, fetching from API", upperSymbol);

        // Fetch from CoinMarketCap API
        CryptoCoinDto coinDto = coinMarketCapService.fetchCoinBySymbol(upperSymbol)
                .orElseThrow(() -> new AuthenticationException("Failed to fetch data for symbol: " + symbol));

        // Store in cache
        saveToCache(cacheKey, coinDto);

        return coinDto;
    }

    /**
     * Get cached coin data
     */
    private CryptoCoinDto getFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached instanceof CryptoCoinDto) {
                    return (CryptoCoinDto) cached;
                }
                // Handle LinkedHashMap from Redis deserialization
                String json = objectMapper.writeValueAsString(cached);
                return objectMapper.readValue(json, CryptoCoinDto.class);
            }
            return null;
        } catch (Exception e) {
            log.error("Error reading from cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Save coin data to cache
     */
    private void saveToCache(String cacheKey, CryptoCoinDto coinDto) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    coinDto,
                    coinMarketCapConfig.getCacheTtlSeconds(),
                    TimeUnit.SECONDS
            );
            log.debug("Saved to cache: {} with TTL: {}s", cacheKey, coinMarketCapConfig.getCacheTtlSeconds());
        } catch (Exception e) {
            log.error("Error saving to cache: {}", e.getMessage());
        }
    }

    /**
     * Invalidate cache for a symbol
     */
    public void invalidateCache(String symbol) {
        String cacheKey = CACHE_KEY_PREFIX + symbol.toUpperCase();
        redisTemplate.delete(cacheKey);
        log.info("Cache invalidated for symbol: {}", symbol);
    }

    /**
     * Check if a symbol is supported
     */
    public boolean isSymbolSupported(String symbol) {
        return SUPPORTED_SYMBOLS.contains(symbol.toUpperCase());
    }
}

