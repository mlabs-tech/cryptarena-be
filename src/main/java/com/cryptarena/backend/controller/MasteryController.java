package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.mastery.ArenaResultRequest;
import com.cryptarena.backend.dto.mastery.ChampionMasteryDto;
import com.cryptarena.backend.dto.mastery.UserMasteryDto;
import com.cryptarena.backend.service.MasteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mastery")
@RequiredArgsConstructor
@Slf4j
public class MasteryController {

    private final MasteryService masteryService;

    @Value("${indexer.api-key:default-indexer-key}")
    private String indexerApiKey;

    /**
     * Endpoint for indexer service to submit arena results
     * Secured via API key in header
     */
    @PostMapping("/internal/arena-result")
    public ResponseEntity<?> processArenaResult(
            @RequestHeader("X-Indexer-Api-Key") String apiKey,
            @RequestBody ArenaResultRequest request) {
        
        // Validate API key
        if (!indexerApiKey.equals(apiKey)) {
            log.warn("Invalid API key attempt for arena result submission");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid API key"));
        }

        try {
            log.info("Received arena result for arena ID: {}", request.getArenaId());
            masteryService.processArenaResult(request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Arena results processed successfully",
                "arenaId", request.getArenaId()
            ));
        } catch (Exception e) {
            log.error("Error processing arena result: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process arena result: " + e.getMessage()));
        }
    }

    /**
     * Get user's mastery profile by user ID (public)
     */
    @GetMapping("/public/user/{userId}")
    public ResponseEntity<UserMasteryDto> getUserMastery(@PathVariable UUID userId) {
        try {
            UserMasteryDto mastery = masteryService.getUserMastery(userId);
            return ResponseEntity.ok(mastery);
        } catch (Exception e) {
            log.error("Error fetching mastery for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user's mastery profile by wallet address (public)
     */
    @GetMapping("/public/wallet/{walletAddress}")
    public ResponseEntity<UserMasteryDto> getUserMasteryByWallet(@PathVariable String walletAddress) {
        try {
            UserMasteryDto mastery = masteryService.getUserMasteryByWallet(walletAddress);
            return ResponseEntity.ok(mastery);
        } catch (Exception e) {
            log.error("Error fetching mastery for wallet {}: {}", walletAddress, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get mastery for a specific champion for a user (public)
     */
    @GetMapping("/public/user/{userId}/champion/{assetIndex}")
    public ResponseEntity<ChampionMasteryDto> getChampionMastery(
            @PathVariable UUID userId,
            @PathVariable Integer assetIndex) {
        try {
            ChampionMasteryDto mastery = masteryService.getChampionMastery(userId, assetIndex);
            return ResponseEntity.ok(mastery);
        } catch (Exception e) {
            log.error("Error fetching champion mastery for user {} asset {}: {}", 
                userId, assetIndex, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current user's mastery (authenticated)
     */
    @GetMapping("/me")
    public ResponseEntity<UserMasteryDto> getCurrentUserMastery(
            @RequestAttribute("userId") UUID userId) {
        try {
            UserMasteryDto mastery = masteryService.getUserMastery(userId);
            return ResponseEntity.ok(mastery);
        } catch (Exception e) {
            log.error("Error fetching mastery for current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

