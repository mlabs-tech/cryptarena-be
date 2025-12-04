package com.cryptarena.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwitterUserResponse {
    
    private TwitterUserData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TwitterUserData {
        private String id;
        private String name;
        private String username;
        
        @JsonProperty("profile_image_url")
        private String profileImageUrl;
    }
}

