package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "champions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Champion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "profile_banner")
    private String profileBanner;

    @Column(name = "profile_character")
    private String profileCharacter;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crypto_coin_id", unique = true)
    private CryptoCoin cryptoCoin;
}

