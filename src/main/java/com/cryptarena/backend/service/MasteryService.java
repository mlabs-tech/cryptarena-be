package com.cryptarena.backend.service;

import com.cryptarena.backend.dto.mastery.ArenaResultRequest;
import com.cryptarena.backend.dto.mastery.ChampionMasteryDto;
import com.cryptarena.backend.dto.mastery.UserMasteryDto;
import com.cryptarena.backend.entity.ChampionMastery;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.Wallet;
import com.cryptarena.backend.repository.ChampionMasteryRepository;
import com.cryptarena.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasteryService {

    private final ChampionMasteryRepository masteryRepository;
    private final WalletRepository walletRepository;

    /**
     * Points awarded based on placement
     * 1st: 100, 2nd: 50, 3rd: 45, 4th: 40, 5th: 35
     * 6th: 30, 7th: 25, 8th: 20, 9th: 15, 10th: 10
     */
    private static final int[] PLACEMENT_POINTS = {100, 50, 45, 40, 35, 30, 25, 20, 15, 10};

    /**
     * Rank thresholds (total mastery points)
     */
    private static final long[] RANK_THRESHOLDS = {
        0,      // Wood
        500,    // Bronze
        1500,   // Silver
        3500,   // Gold
        7000,   // Diamond
        15000,  // Master
        30000   // Grandmaster
    };

    private static final String[] RANK_NAMES = {
        "Wood", "Bronze", "Silver", "Gold", "Diamond", "Master", "Grandmaster"
    };

    /**
     * Champion mastery level thresholds (per champion)
     */
    private static final long[] CHAMPION_LEVEL_THRESHOLDS = {
        0,     // Level 1
        100,   // Level 2
        300,   // Level 3
        600,   // Level 4
        1000,  // Level 5
        2000,  // Level 6
        5000   // Level 7 (Mastery)
    };

    /**
     * Asset symbols for display
     */
    private static final String[] ASSET_SYMBOLS = {
        "SOL", "TRUMP", "PUMP", "BONK", "JUP", "PENGU", "PYTH",
        "HNT", "FARTCOIN", "RAY", "JTO", "KMNO", "MET", "W"
    };

    private static final String[] ASSET_NAMES = {
        "Solana", "Official Trump", "Pump.fun", "Bonk", "Jupiter", "Pudgy Penguins", "Pyth Network",
        "Helium", "Fartcoin", "Raydium", "Jito", "Kamino Finance", "Meteora", "Wormhole"
    };

    /**
     * Process arena results from the indexer and allocate mastery points
     */
    @Transactional
    public void processArenaResult(ArenaResultRequest request) {
        log.info("Processing arena result for arena ID: {} with {} players", 
            request.getArenaId(), request.getResults().size());

        int processed = 0;
        int skipped = 0;
        
        for (ArenaResultRequest.PlayerResult result : request.getResults()) {
            if (processPlayerResult(result)) {
                processed++;
            } else {
                skipped++;
            }
        }

        log.info("Arena {} results: {} players processed, {} skipped (no linked user)", 
            request.getArenaId(), processed, skipped);
    }

    /**
     * Process a single player's result
     * @return true if processed, false if skipped
     */
    private boolean processPlayerResult(ArenaResultRequest.PlayerResult result) {
        // Find the wallet by address
        Optional<Wallet> walletOpt = walletRepository.findByAddress(result.getWalletAddress());
        
        // Skip if wallet not found in database
        if (walletOpt.isEmpty()) {
            log.debug("Wallet not found in database, skipping: {}", result.getWalletAddress());
            return false;
        }

        Wallet wallet = walletOpt.get();
        User user = wallet.getUser();
        
        // Skip if wallet is not linked to a user
        if (user == null) {
            log.debug("Wallet exists but not linked to a user, skipping: {}", result.getWalletAddress());
            return false;
        }

        Integer assetIndex = result.getAssetIndex();
        Integer placement = result.getPlacement();

        // Get or create mastery record
        ChampionMastery mastery = masteryRepository
            .findByUserAndAssetIndex(user, assetIndex)
            .orElseGet(() -> ChampionMastery.builder()
                .user(user)
                .assetIndex(assetIndex)
                .masteryPoints(0L)
                .gamesPlayed(0)
                .wins(0)
                .podiumFinishes(0)
                .build());

        // Calculate points based on placement (1-indexed)
        int pointsEarned = getPointsForPlacement(placement);

        // Update mastery stats
        mastery.setMasteryPoints(mastery.getMasteryPoints() + pointsEarned);
        mastery.setGamesPlayed(mastery.getGamesPlayed() + 1);

        // Update wins (1st place)
        if (placement == 1 || Boolean.TRUE.equals(result.getIsWinner())) {
            mastery.setWins(mastery.getWins() + 1);
        }

        // Update podium finishes (top 3)
        if (placement <= 3) {
            mastery.setPodiumFinishes(mastery.getPodiumFinishes() + 1);
        }

        // Update best placement
        if (mastery.getBestPlacement() == null || placement < mastery.getBestPlacement()) {
            mastery.setBestPlacement(placement);
        }

        masteryRepository.save(mastery);

        log.debug("Awarded {} points to user {} for champion {} (placement: {})",
            pointsEarned, user.getId(), assetIndex, placement);
        
        return true;
    }

    /**
     * Get points for a placement (1-10)
     */
    private int getPointsForPlacement(int placement) {
        if (placement < 1 || placement > PLACEMENT_POINTS.length) {
            return 0;
        }
        return PLACEMENT_POINTS[placement - 1];
    }

    /**
     * Get user's complete mastery profile
     */
    public UserMasteryDto getUserMastery(UUID userId) {
        Long totalPoints = masteryRepository.getTotalMasteryPoints(userId);
        Integer totalGames = masteryRepository.getTotalGamesPlayed(userId);
        Integer totalWins = masteryRepository.getTotalWins(userId);

        List<ChampionMastery> championMasteries = masteryRepository
            .findByUserIdOrderByMasteryPointsDesc(userId);

        List<ChampionMasteryDto> champions = championMasteries.stream()
            .map(this::toChampionMasteryDto)
            .toList();

        double winRate = totalGames > 0 ? (double) totalWins / totalGames * 100 : 0;
        int rankTier = getRankTier(totalPoints);

        return UserMasteryDto.builder()
            .userId(userId)
            .totalMasteryPoints(totalPoints)
            .totalGamesPlayed(totalGames)
            .totalWins(totalWins)
            .winRate(Math.round(winRate * 100.0) / 100.0)
            .rankName(RANK_NAMES[rankTier])
            .rankTier(rankTier + 1) // 1-indexed for frontend
            .champions(champions)
            .build();
    }

    /**
     * Get user's mastery by wallet address
     */
    public UserMasteryDto getUserMasteryByWallet(String walletAddress) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(walletAddress);
        
        if (walletOpt.isEmpty()) {
            // Return empty mastery for unlinked wallets
            return UserMasteryDto.builder()
                .totalMasteryPoints(0L)
                .totalGamesPlayed(0)
                .totalWins(0)
                .winRate(0.0)
                .rankName(RANK_NAMES[0])
                .rankTier(1)
                .champions(Collections.emptyList())
                .build();
        }

        return getUserMastery(walletOpt.get().getUser().getId());
    }

    /**
     * Get mastery for a specific champion for a user
     */
    public ChampionMasteryDto getChampionMastery(UUID userId, Integer assetIndex) {
        return masteryRepository.findByUserIdAndAssetIndex(userId, assetIndex)
            .map(this::toChampionMasteryDto)
            .orElse(createEmptyChampionMastery(assetIndex));
    }

    /**
     * Convert entity to DTO
     */
    private ChampionMasteryDto toChampionMasteryDto(ChampionMastery mastery) {
        int assetIndex = mastery.getAssetIndex();
        int level = getChampionLevel(mastery.getMasteryPoints());
        long pointsToNext = getPointsToNextLevel(mastery.getMasteryPoints());

        return ChampionMasteryDto.builder()
            .id(mastery.getId())
            .assetIndex(assetIndex)
            .symbol(getAssetSymbol(assetIndex))
            .name(getAssetName(assetIndex))
            .masteryPoints(mastery.getMasteryPoints())
            .gamesPlayed(mastery.getGamesPlayed())
            .wins(mastery.getWins())
            .podiumFinishes(mastery.getPodiumFinishes())
            .bestPlacement(mastery.getBestPlacement())
            .masteryLevel(level)
            .pointsToNextLevel(pointsToNext)
            .build();
    }

    /**
     * Create empty mastery DTO for champions not yet played
     */
    private ChampionMasteryDto createEmptyChampionMastery(Integer assetIndex) {
        return ChampionMasteryDto.builder()
            .assetIndex(assetIndex)
            .symbol(getAssetSymbol(assetIndex))
            .name(getAssetName(assetIndex))
            .masteryPoints(0L)
            .gamesPlayed(0)
            .wins(0)
            .podiumFinishes(0)
            .masteryLevel(1)
            .pointsToNextLevel(CHAMPION_LEVEL_THRESHOLDS[1])
            .build();
    }

    /**
     * Get rank tier (0-6) based on total mastery points
     */
    private int getRankTier(long totalPoints) {
        for (int i = RANK_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalPoints >= RANK_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get champion mastery level (1-7) based on points
     */
    private int getChampionLevel(long points) {
        for (int i = CHAMPION_LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (points >= CHAMPION_LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * Get points needed to reach the next level
     */
    private long getPointsToNextLevel(long currentPoints) {
        for (long threshold : CHAMPION_LEVEL_THRESHOLDS) {
            if (currentPoints < threshold) {
                return threshold - currentPoints;
            }
        }
        return 0; // Already at max level
    }

    private String getAssetSymbol(int index) {
        if (index >= 0 && index < ASSET_SYMBOLS.length) {
            return ASSET_SYMBOLS[index];
        }
        return "UNKNOWN";
    }

    private String getAssetName(int index) {
        if (index >= 0 && index < ASSET_NAMES.length) {
            return ASSET_NAMES[index];
        }
        return "Unknown Token";
    }
}

