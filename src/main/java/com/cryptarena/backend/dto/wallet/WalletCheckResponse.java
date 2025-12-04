package com.cryptarena.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCheckResponse {

    private boolean exists;
    private boolean linkedToCurrentUser;
    private boolean linkedToOtherUser;
    private UUID userId;
    private String walletType;
    private WalletDto wallet;
}

