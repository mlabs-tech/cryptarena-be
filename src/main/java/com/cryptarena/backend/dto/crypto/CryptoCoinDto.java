package com.cryptarena.backend.dto.crypto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoCoinDto implements Serializable {

    private String symbol;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal marketCap;
    private BigDecimal percentChange24h;
    private Long lastUpdated;
}

