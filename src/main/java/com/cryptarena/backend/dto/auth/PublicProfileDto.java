package com.cryptarena.backend.dto.auth;

import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public user profile DTO with wallets - for profile pages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDto {
    
    private UUID id;
    private String name;
    private String twitterUsername;
    private String twitterProfilePicture;
    private String profileBanner;
    private List<PublicWalletDto> wallets;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicWalletDto {
        private String address;
        private String walletType;
        private Boolean isPrimary;
        private String chainType;
        private String walletSource;
    }
    
    public static PublicProfileDto fromEntity(User user) {
        List<PublicWalletDto> walletDtos = user.getWallets() != null 
            ? user.getWallets().stream()
                .map(wallet -> PublicWalletDto.builder()
                    .address(wallet.getAddress())
                    .walletType(wallet.getWalletType())
                    .isPrimary(wallet.getIsPrimary())
                    .chainType(wallet.getChainType())
                    .walletSource(wallet.getWalletSource())
                    .build())
                .collect(Collectors.toList())
            : List.of();
            
        return PublicProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .twitterUsername(user.getTwitterUsername())
                .twitterProfilePicture(user.getTwitterProfilePicture())
                .profileBanner(user.getProfileBanner())
                .wallets(walletDtos)
                .build();
    }
}

