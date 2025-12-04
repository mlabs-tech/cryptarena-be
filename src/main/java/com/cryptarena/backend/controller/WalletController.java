package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.auth.PublicUserDto;
import com.cryptarena.backend.dto.wallet.*;
import com.cryptarena.backend.service.JwtService;
import com.cryptarena.backend.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
public class WalletController {

    private final WalletService walletService;
    private final JwtService jwtService;

    /**
     * Get user profile by wallet address (PUBLIC - no auth required)
     * Returns user profile if wallet is linked, or 404 if not found
     */
    @GetMapping("/public/user/{address}")
    public ResponseEntity<PublicUserDto> getUserByWallet(@PathVariable String address) {
        return walletService.getUserByWalletAddress(address)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all wallets for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<WalletDto>> getUserWallets(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        List<WalletDto> wallets = walletService.getUserWallets(userId);
        return ResponseEntity.ok(wallets);
    }

    /**
     * Get user's wallets of a specific type
     */
    @GetMapping("/type/{walletType}")
    public ResponseEntity<List<WalletDto>> getUserWalletsByType(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String walletType) {
        UUID userId = extractUserId(authHeader);
        List<WalletDto> wallets = walletService.getUserWalletsByType(userId, walletType.toUpperCase());
        return ResponseEntity.ok(wallets);
    }

    /**
     * Check if a wallet is already linked
     */
    @GetMapping("/check/{address}")
    public ResponseEntity<WalletCheckResponse> checkWallet(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String address) {
        UUID userId = extractUserId(authHeader);
        WalletCheckResponse response = walletService.checkWallet(address, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate a message for the user to sign
     */
    @GetMapping("/message/{address}")
    public ResponseEntity<GenerateMessageResponse> generateMessage(
            @PathVariable String address) {
        GenerateMessageResponse response = walletService.generateLinkingMessage(address);
        return ResponseEntity.ok(response);
    }

    /**
     * Link a wallet to the authenticated user
     */
    @PostMapping("/link")
    public ResponseEntity<WalletDto> linkWallet(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody LinkWalletRequest request) {
        UUID userId = extractUserId(authHeader);
        WalletDto wallet = walletService.linkWallet(userId, request);
        return ResponseEntity.ok(wallet);
    }

    /**
     * Unlink a wallet from the authenticated user
     */
    @DeleteMapping("/{walletId}")
    public ResponseEntity<Void> unlinkWallet(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID walletId) {
        UUID userId = extractUserId(authHeader);
        walletService.unlinkWallet(userId, walletId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set a wallet as primary
     */
    @PatchMapping("/{walletId}/primary")
    public ResponseEntity<WalletDto> setPrimaryWallet(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID walletId) {
        UUID userId = extractUserId(authHeader);
        WalletDto wallet = walletService.setPrimaryWallet(userId, walletId);
        return ResponseEntity.ok(wallet);
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}

