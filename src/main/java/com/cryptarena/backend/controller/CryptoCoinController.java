package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.crypto.CryptoCoinDto;
import com.cryptarena.backend.service.CryptoCoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
public class CryptoCoinController {

    private final CryptoCoinService cryptoCoinService;

    /**
     * Get cryptocurrency data by symbol from CoinMarketCap
     */
    @GetMapping("/coin/{symbol}")
    public ResponseEntity<CryptoCoinDto> getCoinBySymbol(@PathVariable String symbol) {
        CryptoCoinDto coin = cryptoCoinService.getCoinBySymbol(symbol);
        return ResponseEntity.ok(coin);
    }

    /**
     * Check if a symbol is supported
     */
    @GetMapping("/supported/{symbol}")
    public ResponseEntity<Boolean> isSymbolSupported(@PathVariable String symbol) {
        return ResponseEntity.ok(cryptoCoinService.isSymbolSupported(symbol));
    }
}

