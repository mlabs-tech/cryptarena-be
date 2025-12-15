package com.cryptarena.backend.service;

import com.cryptarena.backend.dto.crypto.CryptoCoinDto;
import com.cryptarena.backend.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoCoinService {

    private final CoinMarketCapService coinMarketCapService;

    // Supported tokens
    private static final Set<String> SUPPORTED_SYMBOLS = Set.of(
            "SOL", "TRUMP", "PUMP", "BONK", "JUP", "PENGU", "PYTH",
            "HNT", "FARTCOIN", "RAY", "JTO", "KMNO", "MET", "W"
    );

    /**
     * Get cryptocurrency data by symbol
     */
    public CryptoCoinDto getCoinBySymbol(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        
        // Validate symbol is supported
        if (!SUPPORTED_SYMBOLS.contains(upperSymbol)) {
            throw new AuthenticationException("Unsupported cryptocurrency symbol: " + symbol);
        }

        log.debug("Fetching coin data for symbol: {}", upperSymbol);

        // Fetch from CoinMarketCap API
        return coinMarketCapService.fetchCoinBySymbol(upperSymbol)
                .orElseThrow(() -> new AuthenticationException("Failed to fetch data for symbol: " + symbol));
    }

    /**
     * Check if a symbol is supported
     */
    public boolean isSymbolSupported(String symbol) {
        return SUPPORTED_SYMBOLS.contains(symbol.toUpperCase());
    }
}
