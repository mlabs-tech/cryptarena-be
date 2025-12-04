package com.cryptarena.backend.dto.quest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Quest definition DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestDto {
    
    private UUID id;
    private String code;
    private String title;
    private String description;
    private String questType; // ONE_TIME or WEEKLY
    private Integer goldReward;
    private Integer requiredAmount;
    private Integer displayOrder;
}

