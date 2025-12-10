package com.cryptarena.backend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkWalletRequest {

    @NotBlank(message = "Wallet address is required")
    private String address;

    @NotBlank(message = "Signature is required")
    private String signature;

    @NotBlank(message = "Message is required")
    private String message;

    @Builder.Default
    private String walletType = "SOLANA";

    @Builder.Default
    private String walletSource = "EXTERNAL";

    @Builder.Default
    private String chainType = "SVM";

    private String label;

    @Builder.Default
    private Boolean isPrimary = false;
}

