package com.cryptarena.backend.service;

import com.cryptarena.backend.config.TwitterOAuthConfig;
import com.cryptarena.backend.dto.auth.TwitterTokenResponse;
import com.cryptarena.backend.dto.auth.TwitterUserResponse;
import com.cryptarena.backend.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwitterOAuthService {

    private final TwitterOAuthConfig twitterConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchange authorization code for access token using PKCE
     */
    public TwitterTokenResponse exchangeCodeForToken(String code, String codeVerifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        // Twitter requires Basic Auth with client_id:client_secret
        String credentials = twitterConfig.getClientId() + ":" + twitterConfig.getClientSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", twitterConfig.getRedirectUri());
        body.add("code_verifier", codeVerifier);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TwitterTokenResponse> response = restTemplate.exchange(
                    twitterConfig.getTokenUrl(),
                    HttpMethod.POST,
                    request,
                    TwitterTokenResponse.class
            );
            
            if (response.getBody() == null) {
                throw new AuthenticationException("Failed to get token response from Twitter");
            }
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to exchange code for token: {}", e.getMessage());
            throw new AuthenticationException("Failed to authenticate with Twitter: " + e.getMessage());
        }
    }

    /**
     * Fetch user profile from Twitter API
     */
    public TwitterUserResponse.TwitterUserData fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // Request user fields including profile image
        String url = twitterConfig.getUserInfoUrl() + "?user.fields=profile_image_url,name,username";

        try {
            ResponseEntity<TwitterUserResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    TwitterUserResponse.class
            );
            
            if (response.getBody() == null || response.getBody().getData() == null) {
                throw new AuthenticationException("Failed to get user profile from Twitter");
            }
            
            return response.getBody().getData();
        } catch (Exception e) {
            log.error("Failed to fetch user profile: {}", e.getMessage());
            throw new AuthenticationException("Failed to fetch Twitter user profile: " + e.getMessage());
        }
    }
}

