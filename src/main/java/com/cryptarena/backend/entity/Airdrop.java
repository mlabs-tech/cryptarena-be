package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "airdrops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airdrop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_address", nullable = false)
    private String walletAddress;

    @Column(name = "amount_sol", nullable = false, precision = 10, scale = 6)
    private BigDecimal amountSol;

    @Column(name = "transaction_signature")
    private String transactionSignature;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "claimed_at", nullable = false)
    @Builder.Default
    private LocalDateTime claimedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

