package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "champion_mastery", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "asset_index"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampionMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Asset index from the Solana program (0-13 for our 14 tokens)
     * This matches the indexer's asset_index
     */
    @Column(name = "asset_index", nullable = false)
    private Integer assetIndex;

    /**
     * Total mastery points accumulated for this champion
     */
    @Column(name = "mastery_points", nullable = false)
    @Builder.Default
    private Long masteryPoints = 0L;

    /**
     * Total games played with this champion
     */
    @Column(name = "games_played", nullable = false)
    @Builder.Default
    private Integer gamesPlayed = 0;

    /**
     * Total wins with this champion (1st place)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer wins = 0;

    /**
     * Total podium finishes (1st, 2nd, or 3rd place)
     */
    @Column(name = "podium_finishes", nullable = false)
    @Builder.Default
    private Integer podiumFinishes = 0;

    /**
     * Best placement ever achieved with this champion
     */
    @Column(name = "best_placement")
    private Integer bestPlacement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

