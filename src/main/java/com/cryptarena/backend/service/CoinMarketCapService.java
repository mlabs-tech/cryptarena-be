package com.cryptarena.backend.service;

import com.cryptarena.backend.config.CoinMarketCapConfig;
import com.cryptarena.backend.dto.crypto.CryptoCoinDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinMarketCapService {

    private final WebClient coinMarketCapWebClient;
    private final CoinMarketCapConfig config;

    /**
     * Fetch cryptocurrency data from CoinMarketCap API by symbol
     */
    public Optional<CryptoCoinDto> fetchCoinBySymbol(String symbol) {
        try {
            log.info("Fetching coin data from CoinMarketCap for symbol: {}", symbol);
            
            JsonNode response = coinMarketCapWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cryptocurrency/quotes/latest")
                            .queryParam("symbol", symbol.toUpperCase())
                            .queryParam("convert", "USD")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("data")) {
                log.warn("No data returned from CoinMarketCap for symbol: {}", symbol);
                return Optional.empty();
            }

            JsonNode data = response.get("data");
            JsonNode coinData = data.get(symbol.toUpperCase());

            if (coinData == null) {
                log.warn("Coin not found in CoinMarketCap response: {}", symbol);
                return Optional.empty();
            }

            JsonNode quote = coinData.get("quote").get("USD");

            CryptoCoinDto dto = CryptoCoinDto.builder()
                    .symbol(coinData.get("symbol").asText())
                    .name(coinData.get("name").asText())
                    .currentPrice(new BigDecimal(quote.get("price").asText()).setScale(2, RoundingMode.HALF_UP))
                    .marketCap(new BigDecimal(quote.get("market_cap").asText()).setScale(0, RoundingMode.HALF_UP))
                    .percentChange24h(new BigDecimal(quote.get("percent_change_24h").asText()).setScale(2, RoundingMode.HALF_UP))
                    .lastUpdated(System.currentTimeMillis())
                    .build();

            log.info("Successfully fetched coin data for {}: price=${}, change24h={}%", 
                    symbol, dto.getCurrentPrice(), dto.getPercentChange24h());
            
            return Optional.of(dto);

        } catch (WebClientResponseException e) {
            log.error("CoinMarketCap API error for symbol {}: {} - {}", 
                    symbol, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching coin data for symbol {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}

