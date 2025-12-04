package com.cryptarena.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "twitter.oauth2")
@Getter
@Setter
public class TwitterOAuthConfig {
    
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUrl = "https://api.twitter.com/2/oauth2/token";
    private String userInfoUrl = "https://api.twitter.com/2/users/me";
}

