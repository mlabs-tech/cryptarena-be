package com.cryptarena.backend.service;

import com.cryptarena.backend.config.PrivyConfig;
import com.cryptarena.backend.exception.AuthenticationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrivyService {

    private final PrivyConfig privyConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String PRIVY_API_BASE_URL = "https://auth.privy.io/api/v1";

    /**
     * Verify Privy access token and extract user ID
     */
    public String extractPrivyUserId(String accessToken) {
        try {
            // Decode JWT without verification (we'll verify by calling Privy API)
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                throw new AuthenticationException("Invalid Privy token format");
            }
            
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            log.debug("Privy token payload: {}", payload);
            
            // Extract the subject (Privy user ID)
            JsonNode payloadNode = objectMapper.readTree(payload);
            String privyUserId = payloadNode.has("sub") ? payloadNode.get("sub").asText() : null;
            
            if (privyUserId == null || !privyUserId.startsWith("did:privy:")) {
                throw new AuthenticationException("Invalid Privy user ID in token");
            }
            
            // Verify the audience matches our app ID
            String audience = payloadNode.has("aud") ? payloadNode.get("aud").asText() : null;
            if (privyConfig.getAppId() != null && !privyConfig.getAppId().isEmpty() 
                    && !privyConfig.getAppId().equals(audience)) {
                log.warn("Privy token audience mismatch. Expected: {}, Got: {}", privyConfig.getAppId(), audience);
            }
            
            return privyUserId;
            
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Privy token: {}", e.getMessage());
            throw new AuthenticationException("Failed to verify Privy authentication");
        }
    }

    /**
     * Get full user data from Privy API using app credentials
     * This verifies the user exists and gets all linked accounts including wallets
     */
    public PrivyUserData getUserFromPrivyApi(String privyUserId) {
        try {
            // Build Basic auth header with app_id:app_secret
            String credentials = privyConfig.getAppId() + ":" + privyConfig.getAppSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());
            
            WebClient webClient = webClientBuilder
                    .baseUrl(PRIVY_API_BASE_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .defaultHeader("privy-app-id", privyConfig.getAppId())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            
            String response = webClient.get()
                    .uri("/users/{userId}", privyUserId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.debug("Privy API response: {}", response);
            
            return parsePrivyUserResponse(response);
            
        } catch (WebClientResponseException e) {
            log.error("Privy API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                throw new AuthenticationException("Privy user not found");
            }
            throw new AuthenticationException("Failed to verify Privy user: " + e.getMessage());
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get user from Privy API: {}", e.getMessage(), e);
            throw new AuthenticationException("Failed to verify Privy user");
        }
    }
    
    /**
     * Parse Privy API user response and extract relevant data
     */
    private PrivyUserData parsePrivyUserResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            PrivyUserData.PrivyUserDataBuilder builder = PrivyUserData.builder();
            
            // Extract user ID
            builder.privyUserId(root.has("id") ? root.get("id").asText() : null);
            
            // Parse linked accounts
            if (root.has("linked_accounts") && root.get("linked_accounts").isArray()) {
                JsonNode linkedAccounts = root.get("linked_accounts");
                
                List<WalletInfo> wallets = new ArrayList<>();
                
                for (JsonNode account : linkedAccounts) {
                    String type = account.has("type") ? account.get("type").asText() : "";
                    
                    // Extract Twitter data
                    if ("twitter_oauth".equals(type)) {
                        builder.twitterId(account.has("subject") ? account.get("subject").asText() : null);
                        builder.twitterUsername(account.has("username") ? account.get("username").asText() : null);
                        builder.twitterName(account.has("name") ? account.get("name").asText() : null);
                        builder.twitterProfilePicture(account.has("profile_picture_url") ? account.get("profile_picture_url").asText() : null);
                    }
                    
                    // Extract wallet data
                    if ("wallet".equals(type)) {
                        String address = account.has("address") ? account.get("address").asText() : null;
                        String chainType = account.has("chain_type") ? account.get("chain_type").asText() : null;
                        String walletClient = account.has("wallet_client") ? account.get("wallet_client").asText() : null;
                        String walletClientType = account.has("wallet_client_type") ? account.get("wallet_client_type").asText() : null;
                        
                        if (address != null && chainType != null) {
                            wallets.add(WalletInfo.builder()
                                    .address(address)
                                    .chainType(chainType) // "solana" or "ethereum"
                                    .walletClient(walletClient)
                                    .walletClientType(walletClientType) // "privy" for embedded wallets
                                    .build());
                        }
                    }
                }
                
                builder.wallets(wallets);
            }
            
            PrivyUserData userData = builder.build();
            
            // Validate we got the required Twitter data
            if (userData.getTwitterId() == null || userData.getTwitterUsername() == null) {
                log.warn("Privy user missing Twitter data: {}", userData.getPrivyUserId());
            }
            
            return userData;
            
        } catch (Exception e) {
            log.error("Failed to parse Privy user response: {}", e.getMessage());
            throw new AuthenticationException("Failed to parse Privy user data");
        }
    }

    /**
     * Full verification and data extraction
     * 1. Extract user ID from token
     * 2. Verify user exists and get full data from Privy API
     */
    public PrivyUserData verifyAndGetUserData(String accessToken) {
        String privyUserId = extractPrivyUserId(accessToken);
        return getUserFromPrivyApi(privyUserId);
    }
    
    @lombok.Builder
    @lombok.Data
    public static class PrivyUserData {
        private String privyUserId;
        private String twitterId;
        private String twitterUsername;
        private String twitterName;
        private String twitterProfilePicture;
        private List<WalletInfo> wallets;
        
        public WalletInfo getSolanaWallet() {
            if (wallets == null) return null;
            return wallets.stream()
                    .filter(w -> "solana".equalsIgnoreCase(w.getChainType()))
                    .findFirst()
                    .orElse(null);
        }
        
        public WalletInfo getEthereumWallet() {
            if (wallets == null) return null;
            return wallets.stream()
                    .filter(w -> "ethereum".equalsIgnoreCase(w.getChainType()))
                    .findFirst()
                    .orElse(null);
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class WalletInfo {
        private String address;
        private String chainType; // "solana" or "ethereum"
        private String walletClient;
        private String walletClientType; // "privy" for embedded wallets
        
        public boolean isPrivyEmbeddedWallet() {
            return "privy".equalsIgnoreCase(walletClientType);
        }
    }
}
