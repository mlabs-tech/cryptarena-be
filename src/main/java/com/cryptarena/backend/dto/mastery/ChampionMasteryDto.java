package com.cryptarena.backend.dto.mastery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for champion mastery data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampionMasteryDto {
    
    private UUID id;
    
    /**
     * Asset index (0-13)
     */
    private Integer assetIndex;
    
    /**
     * Champion symbol (e.g., "SOL", "TRUMP")
     */
    private String symbol;
    
    /**
     * Champion name (e.g., "Solana", "Official Trump")
     */
    private String name;
    
    /**
     * Total mastery points for this champion
     */
    private Long masteryPoints;
    
    /**
     * Games played with this champion
     */
    private Integer gamesPlayed;
    
    /**
     * Wins with this champion
     */
    private Integer wins;
    
    /**
     * Podium finishes (top 3)
     */
    private Integer podiumFinishes;
    
    /**
     * Best placement achieved
     */
    private Integer bestPlacement;
    
    /**
     * Mastery level based on points (1-7)
     */
    private Integer masteryLevel;
    
    /**
     * Points needed for next level
     */
    private Long pointsToNextLevel;
}

