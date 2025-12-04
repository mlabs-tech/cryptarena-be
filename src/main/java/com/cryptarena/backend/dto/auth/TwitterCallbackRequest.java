package com.cryptarena.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwitterCallbackRequest {
    
    @NotBlank(message = "Authorization code is required")
    private String code;
    
    @NotBlank(message = "Code verifier is required for PKCE")
    private String codeVerifier;
}

