package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.Airdrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AirdropRepository extends JpaRepository<Airdrop, UUID> {

    /**
     * Find all airdrops for a specific wallet address ordered by claim time
     */
    List<Airdrop> findByWalletAddressOrderByClaimedAtDesc(String walletAddress);

    /**
     * Find all airdrops for a specific user ordered by claim time
     */
    List<Airdrop> findByUser_IdOrderByClaimedAtDesc(UUID userId);

    /**
     * Find the most recent successful airdrop for a wallet address
     */
    @Query("SELECT a FROM Airdrop a WHERE a.walletAddress = :walletAddress " +
           "AND a.status = 'SUCCESS' ORDER BY a.claimedAt DESC LIMIT 1")
    Optional<Airdrop> findMostRecentSuccessfulAirdrop(@Param("walletAddress") String walletAddress);

    /**
     * Check if wallet has claimed within a specific time window
     */
    @Query("SELECT COUNT(a) > 0 FROM Airdrop a WHERE a.walletAddress = :walletAddress " +
           "AND a.status = 'SUCCESS' AND a.claimedAt > :afterTime")
    boolean hasClaimedAfter(@Param("walletAddress") String walletAddress, 
                           @Param("afterTime") LocalDateTime afterTime);
}

