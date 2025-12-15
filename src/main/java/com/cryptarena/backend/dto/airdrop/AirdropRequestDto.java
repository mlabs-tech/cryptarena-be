package com.cryptarena.backend.dto.airdrop;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirdropRequestDto {
    
    @NotBlank(message = "Wallet address is required")
    private String walletAddress;
}

