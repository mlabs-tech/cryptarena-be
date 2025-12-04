package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Quest definition - defines available quests in the system
 */
@Entity
@Table(name = "quests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    /**
     * Quest type: ONE_TIME or WEEKLY
     */
    @Column(name = "quest_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestType questType;

    /**
     * Gold reward for completing the quest
     */
    @Column(name = "gold_reward", nullable = false)
    private Integer goldReward;

    /**
     * Required amount to complete (e.g., 3 for "Enter 3 arenas")
     */
    @Column(name = "required_amount", nullable = false)
    @Builder.Default
    private Integer requiredAmount = 1;

    /**
     * Whether this quest is currently active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Display order for UI
     */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    public enum QuestType {
        ONE_TIME,
        WEEKLY
    }
}

