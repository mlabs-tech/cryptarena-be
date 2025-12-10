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
public class PrivyAuthRequest {
    
    @NotBlank(message = "Privy access token is required")
    private String accessToken;
}
