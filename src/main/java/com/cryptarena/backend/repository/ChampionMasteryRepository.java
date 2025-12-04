package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.ChampionMastery;
import com.cryptarena.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChampionMasteryRepository extends JpaRepository<ChampionMastery, UUID> {

    /**
     * Find all mastery records for a user
     */
    List<ChampionMastery> findByUser(User user);

    /**
     * Find all mastery records for a user by user ID
     */
    List<ChampionMastery> findByUserId(UUID userId);

    /**
     * Find mastery for a specific user and champion (asset index)
     */
    Optional<ChampionMastery> findByUserAndAssetIndex(User user, Integer assetIndex);

    /**
     * Find mastery for a specific user ID and asset index
     */
    Optional<ChampionMastery> findByUserIdAndAssetIndex(UUID userId, Integer assetIndex);

    /**
     * Get total mastery points for a user across all champions
     */
    @Query("SELECT COALESCE(SUM(cm.masteryPoints), 0) FROM ChampionMastery cm WHERE cm.user.id = :userId")
    Long getTotalMasteryPoints(@Param("userId") UUID userId);

    /**
     * Get total games played by a user
     */
    @Query("SELECT COALESCE(SUM(cm.gamesPlayed), 0) FROM ChampionMastery cm WHERE cm.user.id = :userId")
    Integer getTotalGamesPlayed(@Param("userId") UUID userId);

    /**
     * Get total wins by a user
     */
    @Query("SELECT COALESCE(SUM(cm.wins), 0) FROM ChampionMastery cm WHERE cm.user.id = :userId")
    Integer getTotalWins(@Param("userId") UUID userId);

    /**
     * Get top mastery entries for a specific champion (leaderboard)
     */
    @Query("SELECT cm FROM ChampionMastery cm WHERE cm.assetIndex = :assetIndex ORDER BY cm.masteryPoints DESC")
    List<ChampionMastery> findTopByAssetIndex(@Param("assetIndex") Integer assetIndex);

    /**
     * Get mastery entries for a user ordered by points descending
     */
    @Query("SELECT cm FROM ChampionMastery cm WHERE cm.user.id = :userId ORDER BY cm.masteryPoints DESC")
    List<ChampionMastery> findByUserIdOrderByMasteryPointsDesc(@Param("userId") UUID userId);
}

