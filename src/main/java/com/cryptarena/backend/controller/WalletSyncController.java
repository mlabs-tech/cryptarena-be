package com.cryptarena.backend.controller;

import com.cryptarena.backend.service.JwtService;
import com.cryptarena.backend.service.PrivyWalletSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
public class WalletSyncController {

    private final PrivyWalletSyncService privyWalletSyncService;
    private final JwtService jwtService;

    /**
     * Sync Privy wallets for the current authenticated user
     * This is useful after login to ensure wallets are linked
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncCurrentUserWallets(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            
            if (!jwtService.isTokenValid(token)) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Invalid token"
                ));
            }
            
            UUID userId = jwtService.extractUserId(token);
            boolean synced = privyWalletSyncService.syncUserWalletsById(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "synced", synced,
                    "message", synced ? "Wallets synced successfully" : "No new wallets to sync"
            ));
        } catch (Exception e) {
            log.error("Failed to sync wallets: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to sync wallets: " + e.getMessage()
            ));
        }
    }

    /**
     * Force sync Privy wallets for a specific user (requires valid token)
     * User can only sync their own wallets
     */
    @PostMapping("/sync/{userId}")
    public ResponseEntity<Map<String, Object>> syncUserWallets(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            
            if (!jwtService.isTokenValid(token)) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Invalid token"
                ));
            }
            
            // User can only sync their own wallets
            UUID tokenUserId = jwtService.extractUserId(token);
            if (!tokenUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Cannot sync wallets for another user"
                ));
            }
            
            boolean synced = privyWalletSyncService.syncUserWalletsById(userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "synced", synced,
                    "message", synced ? "Wallets synced successfully" : "No new wallets to sync"
            ));
        } catch (Exception e) {
            log.error("Failed to sync wallets for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to sync wallets: " + e.getMessage()
            ));
        }
    }
}

