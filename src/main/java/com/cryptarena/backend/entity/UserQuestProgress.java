package com.cryptarena.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks user progress on quests
 */
@Entity
@Table(name = "user_quest_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "quest_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    /**
     * Current progress amount (e.g., 2 out of 3 arenas entered)
     */
    @Column(name = "current_amount", nullable = false)
    @Builder.Default
    private Integer currentAmount = 0;

    /**
     * Whether the quest has been completed
     */
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    /**
     * Whether the reward has been claimed
     */
    @Column(name = "reward_claimed", nullable = false)
    @Builder.Default
    private Boolean rewardClaimed = false;

    /**
     * When the quest was completed
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * When the reward was claimed
     */
    @Column(name = "reward_claimed_at")
    private LocalDateTime rewardClaimedAt;

    /**
     * For weekly quests - the week this progress is for
     * Format: YYYY-WW (e.g., 2024-49)
     */
    @Column(name = "week_key")
    private String weekKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if progress is complete
     */
    public boolean checkCompletion() {
        return this.currentAmount >= this.quest.getRequiredAmount();
    }
}

