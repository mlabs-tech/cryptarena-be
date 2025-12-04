package com.cryptarena.backend.dto.mastery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for user's overall mastery profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMasteryDto {
    
    private UUID userId;
    
    /**
     * Total mastery points across all champions
     */
    private Long totalMasteryPoints;
    
    /**
     * Total games played
     */
    private Integer totalGamesPlayed;
    
    /**
     * Total wins (1st place)
     */
    private Integer totalWins;
    
    /**
     * Win rate percentage (0-100)
     */
    private Double winRate;
    
    /**
     * Mastery rank name based on total points
     * (Wood, Bronze, Silver, Gold, Diamond, Master, Grandmaster)
     */
    private String rankName;
    
    /**
     * Rank tier (1-7)
     */
    private Integer rankTier;
    
    /**
     * Mastery for each champion the user has played
     */
    private List<ChampionMasteryDto> champions;
}

