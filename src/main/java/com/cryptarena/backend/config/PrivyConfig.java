package com.cryptarena.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "privy")
@Getter
@Setter
public class PrivyConfig {
    
    private String appId;
    private String appSecret;
    private String verificationKey;
}

