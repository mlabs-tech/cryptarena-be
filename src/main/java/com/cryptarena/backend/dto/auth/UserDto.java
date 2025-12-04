package com.cryptarena.backend.dto.auth;

import com.cryptarena.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    private UUID id;
    private String name;
    private String twitterUsername;
    private String twitterProfilePicture;
    private String profileBanner;
    private BigDecimal gold;
    
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .twitterUsername(user.getTwitterUsername())
                .twitterProfilePicture(user.getTwitterProfilePicture())
                .profileBanner(user.getProfileBanner())
                .gold(user.getGold())
                .build();
    }
}

