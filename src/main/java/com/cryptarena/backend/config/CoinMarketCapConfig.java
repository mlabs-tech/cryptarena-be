package com.cryptarena.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "coinmarketcap")
@Getter
@Setter
public class CoinMarketCapConfig {

    private String apiKey;
    private String baseUrl;

    @Bean
    public WebClient coinMarketCapWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-CMC_PRO_API_KEY", apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}

