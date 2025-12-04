package com.cryptarena.backend.controller;

import com.cryptarena.backend.dto.quest.UserQuestProgressDto;
import com.cryptarena.backend.service.JwtService;
import com.cryptarena.backend.service.QuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
public class QuestController {

    private final QuestService questService;
    private final JwtService jwtService;

    @Value("${admin.api-key:default-admin-key}")
    private String adminApiKey;

    /**
     * Get all quests with current user's progress (authenticated)
     */
    @GetMapping("/me")
    public ResponseEntity<List<UserQuestProgressDto>> getMyQuests(
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID userId = extractUserId(authHeader);
            List<UserQuestProgressDto> quests = questService.getUserQuests(userId);
            return ResponseEntity.ok(quests);
        } catch (Exception e) {
            log.error("Error fetching quests: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get quests for a specific user (public - for profile pages)
     */
    @GetMapping("/public/user/{userId}")
    public ResponseEntity<List<UserQuestProgressDto>> getUserQuests(@PathVariable UUID userId) {
        try {
            List<UserQuestProgressDto> quests = questService.getUserQuests(userId);
            return ResponseEntity.ok(quests);
        } catch (Exception e) {
            log.error("Error fetching quests for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Admin: Reset weekly quests for a specific user
     */
    @PostMapping("/admin/reset-weekly/{userId}")
    public ResponseEntity<?> resetWeeklyQuestsForUser(
            @RequestHeader("X-Admin-Api-Key") String apiKey,
            @PathVariable UUID userId) {
        
        // Validate admin API key
        if (!adminApiKey.equals(apiKey)) {
            log.warn("Invalid admin API key attempt for quest reset");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid admin API key"));
        }

        try {
            questService.resetWeeklyQuestsForUser(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Weekly quests reset for user " + userId
            ));
        } catch (Exception e) {
            log.error("Error resetting quests for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reset quests: " + e.getMessage()));
        }
    }

    /**
     * Admin: Reset all weekly quests for all users
     */
    @PostMapping("/admin/reset-weekly-all")
    public ResponseEntity<?> resetAllWeeklyQuests(
            @RequestHeader("X-Admin-Api-Key") String apiKey) {
        
        // Validate admin API key
        if (!adminApiKey.equals(apiKey)) {
            log.warn("Invalid admin API key attempt for quest reset all");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid admin API key"));
        }

        try {
            questService.resetAllWeeklyQuests();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All weekly quests reset"
            ));
        } catch (Exception e) {
            log.error("Error resetting all quests: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reset quests: " + e.getMessage()));
        }
    }

    private UUID extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
