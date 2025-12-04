package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "crypto_coins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoCoin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(name = "current_price", precision = 30, scale = 10)
    private BigDecimal currentPrice;

    @Column(name = "market_cap", precision = 30, scale = 2)
    private BigDecimal marketCap;

    @OneToOne(mappedBy = "cryptoCoin")
    private Champion champion;
}

