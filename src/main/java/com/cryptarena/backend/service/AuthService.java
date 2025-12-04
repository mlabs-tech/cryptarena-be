package com.cryptarena.backend.service;

import com.cryptarena.backend.config.JwtConfig;
import com.cryptarena.backend.dto.auth.*;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.exception.AuthenticationException;
import com.cryptarena.backend.repository.UserRepository;
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
                .gold(new BigDecimal("1000")) // Starting gold for new users
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
}

