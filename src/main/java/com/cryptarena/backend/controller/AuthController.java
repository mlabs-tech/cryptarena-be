package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.auth.*;
import com.cryptarena.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
public class AuthController {

    private final AuthService authService;

    /**
     * Handle Twitter OAuth callback
     * Frontend sends authorization code and code verifier after user authorizes
     */
    @PostMapping("/twitter/callback")
    public ResponseEntity<AuthResponse> twitterCallback(@Valid @RequestBody TwitterCallbackRequest request) {
        AuthResponse response = authService.handleTwitterCallback(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle Privy authentication
     * Frontend sends Privy access token and user data after Privy authentication
     */
    @PostMapping("/privy/callback")
    public ResponseEntity<AuthResponse> privyCallback(@Valid @RequestBody PrivyAuthRequest request) {
        AuthResponse response = authService.handlePrivyAuth(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UserDto user = authService.getCurrentUser(token);
        return ResponseEntity.ok(user);
    }
}

