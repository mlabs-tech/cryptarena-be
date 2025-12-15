package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.airdrop.AirdropEligibilityDto;
import com.cryptarena.backend.dto.airdrop.AirdropRequestDto;
import com.cryptarena.backend.dto.airdrop.AirdropResponseDto;
import com.cryptarena.backend.service.AirdropService;
import com.cryptarena.backend.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/airdrop")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
@Slf4j
public class AirdropController {

    private final AirdropService airdropService;
    private final JwtService jwtService;

    /**
     * Check if user's wallet is eligible for airdrop
     */
    @GetMapping("/eligibility/{walletAddress}")
    public ResponseEntity<AirdropEligibilityDto> checkEligibility(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String walletAddress) {
        UUID userId = extractUserId(authHeader);
        AirdropEligibilityDto eligibility = airdropService.checkEligibility(userId, walletAddress);
        return ResponseEntity.ok(eligibility);
    }

    /**
     * Claim airdrop (testnet SOL)
     */
    @PostMapping("/claim")
    public ResponseEntity<AirdropResponseDto> claimAirdrop(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AirdropRequestDto request) {
        UUID userId = extractUserId(authHeader);
        log.info("Airdrop claim request from user {} for wallet {}", userId, request.getWalletAddress());
        
        AirdropResponseDto response = airdropService.claimAirdrop(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get airdrop history for authenticated user
     */
    @GetMapping("/history")
    public ResponseEntity<List<AirdropResponseDto>> getAirdropHistory(
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        List<AirdropResponseDto> history = airdropService.getUserAirdropHistory(userId);
        return ResponseEntity.ok(history);
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}

