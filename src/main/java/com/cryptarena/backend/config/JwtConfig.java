package com.cryptarena.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    
    private String secret;
    private long accessTokenExpiration = 900000; // 15 minutes in milliseconds
    private long refreshTokenExpiration = 604800000; // 7 days in milliseconds
}

