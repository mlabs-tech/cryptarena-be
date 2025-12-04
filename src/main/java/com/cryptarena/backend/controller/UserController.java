package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.auth.PublicProfileDto;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get public user profile by user ID (PUBLIC - no auth required)
     * Returns user profile with linked wallets, or 404 if not found
     */
    @GetMapping("/public/{userId}")
    public ResponseEntity<PublicProfileDto> getPublicProfile(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(PublicProfileDto.fromEntity(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get public user profile by Twitter username (PUBLIC - no auth required)
     */
    @GetMapping("/public/twitter/{username}")
    public ResponseEntity<PublicProfileDto> getPublicProfileByTwitter(@PathVariable String username) {
        return userRepository.findByTwitterUsername(username)
                .map(user -> ResponseEntity.ok(PublicProfileDto.fromEntity(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}

