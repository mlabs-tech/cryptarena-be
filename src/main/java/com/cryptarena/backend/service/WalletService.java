package com.cryptarena.backend.service;

import com.cryptarena.backend.dto.auth.PublicUserDto;
import com.cryptarena.backend.dto.wallet.*;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.Wallet;
import com.cryptarena.backend.exception.AuthenticationException;
import com.cryptarena.backend.repository.UserRepository;
import com.cryptarena.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final SolanaSignatureService solanaSignatureService;
    private final QuestService questService;
    
    // Message validity period: 5 minutes
    private static final long MESSAGE_MAX_AGE_MS = 5 * 60 * 1000;

    /**
     * Get user profile by wallet address (PUBLIC)
     */
    public Optional<PublicUserDto> getUserByWalletAddress(String address) {
        return walletRepository.findByAddressIgnoreCase(address)
                .map(wallet -> PublicUserDto.fromEntity(wallet.getUser()));
    }

    /**
     * Get all wallets for a user
     */
    public List<WalletDto> getUserWallets(UUID userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(WalletDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get user's wallets of a specific type
     */
    public List<WalletDto> getUserWalletsByType(UUID userId, String walletType) {
        return walletRepository.findByUserIdAndWalletType(userId, walletType).stream()
                .map(WalletDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Check if a wallet exists and its linking status
     */
    public WalletCheckResponse checkWallet(String address, UUID currentUserId) {
        return walletRepository.findByAddressIgnoreCase(address)
                .map(wallet -> {
                    boolean linkedToCurrentUser = wallet.getUser().getId().equals(currentUserId);
                    return WalletCheckResponse.builder()
                            .exists(true)
                            .linkedToCurrentUser(linkedToCurrentUser)
                            .linkedToOtherUser(!linkedToCurrentUser)
                            .userId(wallet.getUser().getId())
                            .walletType(wallet.getWalletType())
                            .wallet(linkedToCurrentUser ? WalletDto.fromEntity(wallet) : null)
                            .build();
                })
                .orElse(WalletCheckResponse.builder()
                        .exists(false)
                        .linkedToCurrentUser(false)
                        .linkedToOtherUser(false)
                        .build());
    }

    /**
     * Generate a message for the user to sign
     */
    public GenerateMessageResponse generateLinkingMessage(String walletAddress) {
        String nonce = generateNonce();
        long timestamp = System.currentTimeMillis();
        String message = solanaSignatureService.generateSigningMessage(walletAddress, nonce, timestamp);
        
        return GenerateMessageResponse.builder()
                .message(message)
                .timestamp(timestamp)
                .nonce(nonce)
                .build();
    }

    /**
     * Link a wallet to a user after verifying signature
     */
    @Transactional
    public WalletDto linkWallet(UUID userId, LinkWalletRequest request) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Check if wallet is already linked
        WalletCheckResponse checkResponse = checkWallet(request.getAddress(), userId);
        
        if (checkResponse.isLinkedToOtherUser()) {
            throw new AuthenticationException("This wallet is already linked to another account");
        }
        
        if (checkResponse.isLinkedToCurrentUser()) {
            log.info("Wallet {} is already linked to user {}", request.getAddress(), userId);
            return checkResponse.getWallet();
        }

        // Extract timestamp from message and validate
        Long timestamp = extractTimestampFromMessage(request.getMessage());
        if (timestamp == null || !solanaSignatureService.isMessageTimestampValid(timestamp, MESSAGE_MAX_AGE_MS)) {
            throw new AuthenticationException("Signature message has expired. Please try again.");
        }

        // Verify signature
        boolean isValid = solanaSignatureService.verifySignature(
                request.getAddress(),
                request.getMessage(),
                request.getSignature()
        );

        if (!isValid) {
            throw new AuthenticationException("Invalid wallet signature. Please try again.");
        }

        // If this is the first wallet, make it primary
        boolean shouldBePrimary = request.getIsPrimary() != null && request.getIsPrimary();
        if (!shouldBePrimary) {
            List<Wallet> existingWallets = walletRepository.findByUserIdAndWalletType(userId, request.getWalletType());
            shouldBePrimary = existingWallets.isEmpty();
        }

        // If setting as primary, unset other primary wallets of same type
        if (shouldBePrimary) {
            walletRepository.findByUserIdAndIsPrimaryTrueAndWalletType(userId, request.getWalletType())
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setIsPrimary(false);
                        walletRepository.save(existingPrimary);
                    });
        }

        // Create and save the wallet
        Wallet wallet = Wallet.builder()
                .user(user)
                .address(request.getAddress())
                .walletType(request.getWalletType())
                .walletSource(request.getWalletSource() != null ? request.getWalletSource() : "EXTERNAL")
                .chainType(request.getChainType() != null ? request.getChainType() : "SVM")
                .label(request.getLabel())
                .isPrimary(shouldBePrimary)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Wallet {} linked to user {}", request.getAddress(), userId);

        // Trigger quest completion for linking wallet
        try {
            questService.onWalletLinked(user);
        } catch (Exception e) {
            log.warn("Failed to update quest for wallet linking: {}", e.getMessage());
            // Don't fail the wallet linking if quest update fails
        }

        return WalletDto.fromEntity(wallet);
    }

    /**
     * Unlink a wallet from a user
     */
    @Transactional
    public void unlinkWallet(UUID userId, UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new AuthenticationException("Wallet not found"));

        if (!wallet.getUser().getId().equals(userId)) {
            throw new AuthenticationException("You don't have permission to unlink this wallet");
        }

        walletRepository.delete(wallet);
        log.info("Wallet {} unlinked from user {}", wallet.getAddress(), userId);

        // If this was the primary wallet, set another one as primary
        if (wallet.getIsPrimary()) {
            walletRepository.findByUserIdAndWalletType(userId, wallet.getWalletType()).stream()
                    .findFirst()
                    .ifPresent(nextWallet -> {
                        nextWallet.setIsPrimary(true);
                        walletRepository.save(nextWallet);
                    });
        }
    }

    /**
     * Set a wallet as primary
     */
    @Transactional
    public WalletDto setPrimaryWallet(UUID userId, UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new AuthenticationException("Wallet not found"));

        if (!wallet.getUser().getId().equals(userId)) {
            throw new AuthenticationException("You don't have permission to modify this wallet");
        }

        // Unset existing primary wallet of same type
        walletRepository.findByUserIdAndIsPrimaryTrueAndWalletType(userId, wallet.getWalletType())
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    walletRepository.save(existingPrimary);
                });

        // Set this wallet as primary
        wallet.setIsPrimary(true);
        wallet = walletRepository.save(wallet);

        return WalletDto.fromEntity(wallet);
    }

    /**
     * Generate a secure random nonce
     */
    private String generateNonce() {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
    }

    /**
     * Extract timestamp from the signing message
     */
    private Long extractTimestampFromMessage(String message) {
        try {
            String[] lines = message.split("\n");
            for (String line : lines) {
                if (line.startsWith("Timestamp: ")) {
                    return Long.parseLong(line.substring("Timestamp: ".length()).trim());
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to extract timestamp from message: {}", e.getMessage());
            return null;
        }
    }
}

