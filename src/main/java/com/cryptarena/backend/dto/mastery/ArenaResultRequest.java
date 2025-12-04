package com.cryptarena.backend.dto.mastery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request from indexer service when an arena ends
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArenaResultRequest {
    
    /**
     * Arena ID from the Solana program
     */
    private Long arenaId;
    
    /**
     * List of player results ordered by placement (1st place first)
     */
    private List<PlayerResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlayerResult {
        /**
         * Solana wallet address of the player
         */
        private String walletAddress;
        
        /**
         * Asset index (champion) the player used
         */
        private Integer assetIndex;
        
        /**
         * Placement in the arena (1-10)
         */
        private Integer placement;
        
        /**
         * Whether this player was a winner (their token had highest volatility)
         */
        private Boolean isWinner;
        
        /**
         * Token amount the player entered with
         */
        private Double tokenAmount;
        
        /**
         * USD value at entry
         */
        private Double usdValue;
    }
}

