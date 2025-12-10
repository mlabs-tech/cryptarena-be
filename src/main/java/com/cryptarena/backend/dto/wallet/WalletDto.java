package com.cryptarena.backend.dto.wallet;

import com.cryptarena.backend.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {

    private UUID id;
    private String address;
    private String walletType;
    private String walletSource;
    private String chainType;
    private String label;
    private Boolean isPrimary;
    private LocalDateTime createdAt;

    public static WalletDto fromEntity(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .address(wallet.getAddress())
                .walletType(wallet.getWalletType())
                .walletSource(wallet.getWalletSource())
                .chainType(wallet.getChainType())
                .label(wallet.getLabel())
                .isPrimary(wallet.getIsPrimary())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}

