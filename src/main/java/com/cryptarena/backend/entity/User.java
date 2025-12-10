package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "twitter_name")
    private String twitterName;

    @Column(name = "twitter_id", unique = true)
    private String twitterId;

    @Column(name = "twitter_profile_picture")
    private String twitterProfilePicture;

    @Column(name = "twitter_username", unique = true)
    private String twitterUsername;

    @Column(name = "privy_id", unique = true)
    private String privyId;

    @Column(precision = 30, scale = 2)
    @Builder.Default
    private BigDecimal gold = BigDecimal.ZERO;

    @Column(name = "profile_banner")
    private String profileBanner;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Wallet> wallets = new ArrayList<>();
}

