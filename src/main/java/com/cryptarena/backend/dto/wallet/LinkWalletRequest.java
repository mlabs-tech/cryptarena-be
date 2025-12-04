package com.cryptarena.backend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkWalletRequest {

    @NotBlank(message = "Wallet address is required")
    private String address;

    @NotBlank(message = "Signature is required")
    private String signature;

    @NotBlank(message = "Message is required")
    private String message;

    private String walletType = "SOLANA";

    private String label;

    private Boolean isPrimary = false;
}

