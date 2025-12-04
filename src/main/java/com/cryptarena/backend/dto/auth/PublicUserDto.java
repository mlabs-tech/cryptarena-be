package com.cryptarena.backend.dto.auth;

import com.cryptarena.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public user profile DTO - does not include sensitive data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserDto {
    
    private String twitterUsername;
    private String twitterProfilePicture;
    private String profileBanner;
    private String name;
    
    public static PublicUserDto fromEntity(User user) {
        return PublicUserDto.builder()
                .name(user.getName())
                .twitterUsername(user.getTwitterUsername())
                .twitterProfilePicture(user.getTwitterProfilePicture())
                .profileBanner(user.getProfileBanner())
                .build();
    }
}

