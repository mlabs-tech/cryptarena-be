package com.cryptarena.backend.service;

import com.cryptarena.backend.config.JwtConfig;
import com.cryptarena.backend.dto.auth.*;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.Wallet;
import com.cryptarena.backend.exception.AuthenticationException;
import com.cryptarena.backend.repository.UserRepository;
import com.cryptarena.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final TwitterOAuthService twitterOAuthService;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PrivyService privyService;

    /**
     * Handle Twitter OAuth callback - exchange code for tokens and create/update user
     */
    @Transactional
    public AuthResponse handleTwitterCallback(TwitterCallbackRequest request) {
        // Exchange authorization code for Twitter access token
        TwitterTokenResponse tokenResponse = twitterOAuthService.exchangeCodeForToken(
                request.getCode(),
                request.getCodeVerifier()
        );

        // Fetch user profile from Twitter
        TwitterUserResponse.TwitterUserData twitterUser = twitterOAuthService.fetchUserProfile(
                tokenResponse.getAccessToken()
        );

        // Create or update user in database
        User user = userRepository.findByTwitterId(twitterUser.getId())
                .map(existingUser -> updateUser(existingUser, twitterUser))
                .orElseGet(() -> createUser(twitterUser));

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User authenticated successfully: {}", user.getTwitterUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtConfig.getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // Check token type
        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new AuthenticationException("Invalid token type");
        }

        // Get user from token
        UUID userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .expiresIn(jwtConfig.getAccessTokenExpiration() / 1000)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Get current user from access token
     */
    public UserDto getCurrentUser(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new AuthenticationException("Invalid access token");
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return UserDto.fromEntity(user);
    }

    private User createUser(TwitterUserResponse.TwitterUserData twitterUser) {
        // Get higher resolution profile image (remove _normal suffix)
        String profileImageUrl = twitterUser.getProfileImageUrl();
        if (profileImageUrl != null) {
            profileImageUrl = profileImageUrl.replace("_normal", "_400x400");
        }

        User user = User.builder()
                .name(twitterUser.getName())
                .twitterId(twitterUser.getId())
                .twitterUsername(twitterUser.getUsername())
                .twitterName(twitterUser.getName())
                .twitterProfilePicture(profileImageUrl)
                .gold(BigDecimal.ZERO)
                .build();

        return userRepository.save(user);
    }

    private User updateUser(User user, TwitterUserResponse.TwitterUserData twitterUser) {
        // Get higher resolution profile image
        String profileImageUrl = twitterUser.getProfileImageUrl();
        if (profileImageUrl != null) {
            profileImageUrl = profileImageUrl.replace("_normal", "_400x400");
        }

        user.setName(twitterUser.getName());
        user.setTwitterName(twitterUser.getName());
        user.setTwitterUsername(twitterUser.getUsername());
        user.setTwitterProfilePicture(profileImageUrl);

        return userRepository.save(user);
    }

    /**
     * Handle Privy authentication - verify token via Privy API and create/update user with embedded wallets
     */
    @Transactional
    public AuthResponse handlePrivyAuth(PrivyAuthRequest request) {
        // Verify the Privy access token and get full user data from Privy API
        // This validates the token and retrieves wallets directly from Privy
        PrivyService.PrivyUserData privyUserData = privyService.verifyAndGetUserData(request.getAccessToken());
        
        // Validate the Privy user has Twitter linked
        if (privyUserData.getTwitterId() == null || privyUserData.getTwitterUsername() == null) {
            throw new AuthenticationException("Twitter account not linked in Privy. Please link your X account.");
        }
        
        log.info("Privy user verified: {} with Twitter: {}", 
                privyUserData.getPrivyUserId(), privyUserData.getTwitterUsername());

        // First, try to find user by Twitter ID (existing user who previously signed up without Privy)
        User user = userRepository.findByTwitterId(privyUserData.getTwitterId())
                .map(existingUser -> updateUserWithPrivy(existingUser, privyUserData))
                .orElseGet(() -> {
                    // If no user found by Twitter ID, check by Privy ID
                    return userRepository.findByPrivyId(privyUserData.getPrivyUserId())
                            .map(existingUser -> updateUserFromPrivy(existingUser, privyUserData))
                            .orElseGet(() -> createUserFromPrivy(privyUserData));
                });

        // Link embedded wallets from Privy API data
        linkPrivyWalletsFromApiData(user, privyUserData);

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User authenticated via Privy: {}", user.getTwitterUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtConfig.getAccessTokenExpiration() / 1000)
                .user(UserDto.fromEntity(user))
                .build();
    }

    private User createUserFromPrivy(PrivyService.PrivyUserData privyData) {
        // Get higher resolution profile image
        String profileImageUrl = privyData.getTwitterProfilePicture();
        if (profileImageUrl != null) {
            profileImageUrl = profileImageUrl.replace("_normal", "_400x400");
        }

        String displayName = privyData.getTwitterName() != null 
                ? privyData.getTwitterName() 
                : privyData.getTwitterUsername();

        User user = User.builder()
                .name(displayName)
                .twitterId(privyData.getTwitterId())
                .twitterUsername(privyData.getTwitterUsername())
                .twitterName(privyData.getTwitterName())
                .twitterProfilePicture(profileImageUrl)
                .privyId(privyData.getPrivyUserId())
                .gold(BigDecimal.ZERO)
                .build();

        return userRepository.save(user);
    }

    private User updateUserWithPrivy(User user, PrivyService.PrivyUserData privyData) {
        // Update user profile from Privy data
        String profileImageUrl = privyData.getTwitterProfilePicture();
        if (profileImageUrl != null) {
            profileImageUrl = profileImageUrl.replace("_normal", "_400x400");
        }

        String displayName = privyData.getTwitterName() != null 
                ? privyData.getTwitterName() 
                : privyData.getTwitterUsername();

        user.setName(displayName);
        user.setTwitterName(privyData.getTwitterName());
        user.setTwitterUsername(privyData.getTwitterUsername());
        user.setTwitterProfilePicture(profileImageUrl);
        user.setPrivyId(privyData.getPrivyUserId());

        return userRepository.save(user);
    }

    private User updateUserFromPrivy(User user, PrivyService.PrivyUserData privyData) {
        // Update existing Privy user's profile
        String profileImageUrl = privyData.getTwitterProfilePicture();
        if (profileImageUrl != null) {
            profileImageUrl = profileImageUrl.replace("_normal", "_400x400");
        }

        String displayName = privyData.getTwitterName() != null 
                ? privyData.getTwitterName() 
                : privyData.getTwitterUsername();

        user.setName(displayName);
        user.setTwitterName(privyData.getTwitterName());
        user.setTwitterUsername(privyData.getTwitterUsername());
        user.setTwitterProfilePicture(profileImageUrl);
        
        // Update Twitter ID if not set
        if (user.getTwitterId() == null) {
            user.setTwitterId(privyData.getTwitterId());
        }

        return userRepository.save(user);
    }

    /**
     * Link wallets from Privy API data (more secure - data comes directly from Privy)
     */
    private void linkPrivyWalletsFromApiData(User user, PrivyService.PrivyUserData privyData) {
        log.info("linkPrivyWalletsFromApiData called for user: {}", user.getId());
        
        if (privyData.getWallets() == null || privyData.getWallets().isEmpty()) {
            log.warn("No wallets found in Privy data for user: {} (privyId: {})", user.getId(), privyData.getPrivyUserId());
            return;
        }

        log.info("Processing {} wallets from Privy for user: {}", privyData.getWallets().size(), user.getId());
        
        for (PrivyService.WalletInfo walletInfo : privyData.getWallets()) {
            log.info("Processing wallet: {} with chainType: {}", walletInfo.getAddress(), walletInfo.getChainType());
            
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
            
            log.info("Calling linkPrivyWallet for address: {}, type: {}, chain: {}, isEmbedded: {}", 
                    walletInfo.getAddress(), walletType, chainType, walletInfo.isPrivyEmbeddedWallet());
            
            linkPrivyWallet(user, walletInfo.getAddress(), walletType, chainType, walletInfo.isPrivyEmbeddedWallet());
        }
        
        log.info("Finished linking wallets for user: {}", user.getId());
    }

    private void linkPrivyWallet(User user, String walletAddress, String walletType, String chainType, boolean isEmbedded) {
        // Check if wallet already exists
        var existingWallet = walletRepository.findByAddressIgnoreCase(walletAddress);
        
        if (existingWallet.isPresent()) {
            Wallet wallet = existingWallet.get();
            // If wallet belongs to this user, update it
            if (wallet.getUser().getId().equals(user.getId())) {
                wallet.setWalletSource("PRIVY");
                wallet.setChainType(chainType);
                if (isEmbedded) {
                    wallet.setLabel("Privy Embedded Wallet");
                }
                walletRepository.save(wallet);
                log.info("Updated existing wallet {} for user {} as PRIVY wallet", walletAddress, user.getId());
            } else {
                // Wallet belongs to another user - this shouldn't happen with Privy embedded wallets
                log.warn("Privy wallet {} already linked to another user (user {})", walletAddress, wallet.getUser().getId());
            }
            return;
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
                .isPrimary(!hasPrimaryWallet) // Make primary if no other primary exists
                .build();

        walletRepository.save(wallet);
        log.info("Linked Privy {} wallet {} to user {} (embedded: {})", chainType, walletAddress, user.getId(), isEmbedded);
    }
}

