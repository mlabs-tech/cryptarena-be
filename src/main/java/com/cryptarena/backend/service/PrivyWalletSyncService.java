package com.cryptarena.backend.service;

import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.Wallet;
import com.cryptarena.backend.repository.UserRepository;
import com.cryptarena.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrivyWalletSyncService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PrivyService privyService;

    /**
     * Scheduled job to sync wallets for Privy users who might be missing wallets
     * Runs every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    @Transactional
    public void syncMissingPrivyWallets() {
        log.debug("Starting Privy wallet sync job...");
        
        // Find all users with Privy ID
        List<User> privyUsers = userRepository.findByPrivyIdIsNotNull();
        
        int syncedCount = 0;
        for (User user : privyUsers) {
            try {
                if (syncUserWalletsIfNeeded(user)) {
                    syncedCount++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync wallets for user {} (privy: {}): {}", 
                        user.getId(), user.getPrivyId(), e.getMessage());
            }
        }
        
        if (syncedCount > 0) {
            log.info("Privy wallet sync completed. Synced wallets for {} users.", syncedCount);
        }
    }

    /**
     * Check if user needs wallet sync and perform it if necessary
     * @return true if wallets were synced
     */
    @Transactional
    public boolean syncUserWalletsIfNeeded(User user) {
        if (user.getPrivyId() == null || user.getPrivyId().isEmpty()) {
            return false;
        }

        // Check if user has both SVM and EVM Privy wallets
        boolean hasSvmPrivyWallet = walletRepository.existsByUserIdAndChainTypeAndWalletSource(
                user.getId(), "SVM", "PRIVY");
        boolean hasEvmPrivyWallet = walletRepository.existsByUserIdAndChainTypeAndWalletSource(
                user.getId(), "EVM", "PRIVY");

        if (hasSvmPrivyWallet && hasEvmPrivyWallet) {
            // User already has both wallet types
            return false;
        }

        log.info("User {} missing Privy wallets (SVM: {}, EVM: {}). Syncing from Privy API...", 
                user.getId(), hasSvmPrivyWallet, hasEvmPrivyWallet);

        return syncUserWallets(user);
    }

    /**
     * Force sync wallets for a user from Privy API
     */
    @Transactional
    public boolean syncUserWallets(User user) {
        if (user.getPrivyId() == null || user.getPrivyId().isEmpty()) {
            log.warn("Cannot sync wallets for user {} - no Privy ID", user.getId());
            return false;
        }

        try {
            // Fetch fresh data from Privy API
            PrivyService.PrivyUserData privyData = privyService.getUserFromPrivyApi(user.getPrivyId());
            
            if (privyData.getWallets() == null || privyData.getWallets().isEmpty()) {
                log.info("No wallets found in Privy for user {} (privy: {})", user.getId(), user.getPrivyId());
                return false;
            }

            int linkedCount = 0;
            for (PrivyService.WalletInfo walletInfo : privyData.getWallets()) {
                String walletType;
                String chainType;
                
                if ("solana".equalsIgnoreCase(walletInfo.getChainType())) {
                    walletType = "SOLANA";
                    chainType = "SVM";
                } else if ("ethereum".equalsIgnoreCase(walletInfo.getChainType())) {
                    walletType = "ETHEREUM";
                    chainType = "EVM";
                } else {
                    log.warn("Unknown chain type: {} for wallet: {}", walletInfo.getChainType(), walletInfo.getAddress());
                    continue;
                }
                
                if (linkPrivyWallet(user, walletInfo.getAddress(), walletType, chainType, walletInfo.isPrivyEmbeddedWallet())) {
                    linkedCount++;
                }
            }

            log.info("Synced {} wallets for user {} from Privy", linkedCount, user.getId());
            return linkedCount > 0;

        } catch (Exception e) {
            log.error("Failed to sync wallets for user {} from Privy: {}", user.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Sync wallets for a specific user by ID
     */
    @Transactional
    public boolean syncUserWalletsById(UUID userId) {
        return userRepository.findById(userId)
                .map(this::syncUserWallets)
                .orElse(false);
    }

    /**
     * Link a single Privy wallet to user
     * @return true if wallet was created (not already existing)
     */
    private boolean linkPrivyWallet(User user, String walletAddress, String walletType, String chainType, boolean isEmbedded) {
        // Check if wallet already exists
        var existingWallet = walletRepository.findByAddressIgnoreCase(walletAddress);
        
        if (existingWallet.isPresent()) {
            Wallet wallet = existingWallet.get();
            // If wallet belongs to this user, update it
            if (wallet.getUser().getId().equals(user.getId())) {
                // Only update if it's not already marked as PRIVY
                if (!"PRIVY".equals(wallet.getWalletSource())) {
                    wallet.setWalletSource("PRIVY");
                    wallet.setChainType(chainType);
                    if (isEmbedded) {
                        wallet.setLabel("Privy Embedded Wallet");
                    }
                    walletRepository.save(wallet);
                    log.info("Updated existing wallet {} for user {} as PRIVY wallet", walletAddress, user.getId());
                    return true;
                }
                // Wallet already exists and is already PRIVY
                return false;
            } else {
                // Wallet belongs to another user
                log.warn("Privy wallet {} already linked to another user (user {})", walletAddress, wallet.getUser().getId());
                return false;
            }
        }

        // Check if user already has a primary wallet of this type
        boolean hasPrimaryWallet = walletRepository.findByUserIdAndIsPrimaryTrueAndWalletType(
                user.getId(), walletType).isPresent();

        // Create new wallet
        Wallet wallet = Wallet.builder()
                .user(user)
                .address(walletAddress)
                .walletType(walletType)
                .walletSource("PRIVY")
                .chainType(chainType)
                .label(isEmbedded ? "Privy Embedded Wallet" : "Privy Linked Wallet")
                .isPrimary(!hasPrimaryWallet)
                .build();

        walletRepository.save(wallet);
        log.info("Linked Privy {} wallet {} to user {} (embedded: {})", chainType, walletAddress, user.getId(), isEmbedded);
        return true;
    }
}

