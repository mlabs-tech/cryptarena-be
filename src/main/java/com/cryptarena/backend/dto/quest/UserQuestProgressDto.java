package com.cryptarena.backend.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User's progress on a specific quest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuestProgressDto {
    
    private UUID progressId;
    
    // Quest details
    private UUID questId;
    private String code;
    private String title;
    private String description;
    private String questType;
    private Integer goldReward;
    
    // Progress
    private Integer currentAmount;
    private Integer requiredAmount;
    private Boolean isCompleted;
    private Boolean rewardClaimed;
    private LocalDateTime completedAt;
    
    // Computed
    private Double progressPercent;
}

