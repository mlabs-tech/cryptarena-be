package com.cryptarena.backend.service;

import com.cryptarena.backend.dto.quest.UserQuestProgressDto;
import com.cryptarena.backend.entity.Quest;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.UserQuestProgress;
import com.cryptarena.backend.repository.QuestRepository;
import com.cryptarena.backend.repository.UserQuestProgressRepository;
import com.cryptarena.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestService {

    private final QuestRepository questRepository;
    private final UserQuestProgressRepository progressRepository;
    private final UserRepository userRepository;

    // Quest codes
    public static final String QUEST_LINK_WALLET = "LINK_WALLET";
    public static final String QUEST_ENTER_ARENAS = "ENTER_3_ARENAS";
    public static final String QUEST_WIN_ARENA = "WIN_ARENA";

    /**
     * Get current week key for weekly quests (format: YYYY-WW)
     */
    public String getCurrentWeekKey() {
        LocalDate now = LocalDate.now();
        int week = now.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = now.get(WeekFields.ISO.weekBasedYear());
        return String.format("%d-%02d", year, week);
    }

    /**
     * Get all quests with user progress
     */
    public List<UserQuestProgressDto> getUserQuests(UUID userId) {
        log.debug("Fetching quests for user: {}", userId);
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return Collections.emptyList();
        }

        List<Quest> allQuests = questRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        log.debug("Found {} active quests in database", allQuests.size());
        
        String currentWeek = getCurrentWeekKey();
        List<UserQuestProgressDto> result = new ArrayList<>();

        for (Quest quest : allQuests) {
            UserQuestProgress progress = getOrCreateProgress(user, quest, currentWeek);
            result.add(toDto(progress));
        }

        log.debug("Returning {} quest progress items for user {}", result.size(), userId);
        return result;
    }

    /**
     * Get or create progress record for a quest
     */
    private UserQuestProgress getOrCreateProgress(User user, Quest quest, String weekKey) {
        if (quest.getQuestType() == Quest.QuestType.WEEKLY) {
            return progressRepository.findByUserAndQuestAndWeekKey(user, quest, weekKey)
                .orElseGet(() -> UserQuestProgress.builder()
                    .user(user)
                    .quest(quest)
                    .currentAmount(0)
                    .isCompleted(false)
                    .rewardClaimed(false)
                    .weekKey(weekKey)
                    .build());
        } else {
            return progressRepository.findByUserAndQuest(user, quest)
                .orElseGet(() -> UserQuestProgress.builder()
                    .user(user)
                    .quest(quest)
                    .currentAmount(0)
                    .isCompleted(false)
                    .rewardClaimed(false)
                    .build());
        }
    }

    /**
     * Called when a user links a wallet for the first time
     */
    @Transactional
    public void onWalletLinked(User user) {
        Quest quest = questRepository.findByCode(QUEST_LINK_WALLET).orElse(null);
        if (quest == null || !quest.getIsActive()) {
            log.debug("LINK_WALLET quest not found or inactive");
            return;
        }

        UserQuestProgress progress = progressRepository.findByUserAndQuest(user, quest)
            .orElseGet(() -> UserQuestProgress.builder()
                .user(user)
                .quest(quest)
                .currentAmount(0)
                .isCompleted(false)
                .rewardClaimed(false)
                .build());

        if (progress.getIsCompleted()) {
            log.debug("LINK_WALLET quest already completed for user {}", user.getId());
            return;
        }

        // Complete the quest
        progress.setCurrentAmount(1);
        progress.setIsCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        
        // Auto-claim reward for one-time quests
        awardGoldReward(user, quest, progress);
        
        progressRepository.save(progress);
        log.info("Completed LINK_WALLET quest for user {}, awarded {} gold", user.getId(), quest.getGoldReward());
    }

    /**
     * Called from MasteryService when processing arena results
     * Updates arena-related quests (enter arenas, win arena)
     */
    @Transactional
    public void onArenaParticipated(User user, boolean isWinner) {
        String currentWeek = getCurrentWeekKey();

        // Update "Enter 3 arenas" quest
        updateEnterArenasQuest(user, currentWeek);

        // Update "Win arena" quest if player won
        if (isWinner) {
            updateWinArenaQuest(user, currentWeek);
        }
    }

    /**
     * Update progress for "Enter 3 arenas" quest
     */
    private void updateEnterArenasQuest(User user, String weekKey) {
        Quest quest = questRepository.findByCode(QUEST_ENTER_ARENAS).orElse(null);
        if (quest == null || !quest.getIsActive()) {
            return;
        }

        UserQuestProgress progress = progressRepository.findByUserAndQuestAndWeekKey(user, quest, weekKey)
            .orElseGet(() -> UserQuestProgress.builder()
                .user(user)
                .quest(quest)
                .currentAmount(0)
                .isCompleted(false)
                .rewardClaimed(false)
                .weekKey(weekKey)
                .build());

        if (progress.getIsCompleted()) {
            return; // Already completed this week
        }

        // Increment progress
        progress.setCurrentAmount(progress.getCurrentAmount() + 1);

        // Check if completed
        if (progress.getCurrentAmount() >= quest.getRequiredAmount()) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            awardGoldReward(user, quest, progress);
            log.info("Completed ENTER_3_ARENAS quest for user {}, awarded {} gold", user.getId(), quest.getGoldReward());
        }

        progressRepository.save(progress);
    }

    /**
     * Update progress for "Win arena" quest
     */
    private void updateWinArenaQuest(User user, String weekKey) {
        Quest quest = questRepository.findByCode(QUEST_WIN_ARENA).orElse(null);
        if (quest == null || !quest.getIsActive()) {
            return;
        }

        UserQuestProgress progress = progressRepository.findByUserAndQuestAndWeekKey(user, quest, weekKey)
            .orElseGet(() -> UserQuestProgress.builder()
                .user(user)
                .quest(quest)
                .currentAmount(0)
                .isCompleted(false)
                .rewardClaimed(false)
                .weekKey(weekKey)
                .build());

        if (progress.getIsCompleted()) {
            return; // Already completed this week
        }

        // Increment progress
        progress.setCurrentAmount(progress.getCurrentAmount() + 1);

        // Check if completed
        if (progress.getCurrentAmount() >= quest.getRequiredAmount()) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            awardGoldReward(user, quest, progress);
            log.info("Completed WIN_ARENA quest for user {}, awarded {} gold", user.getId(), quest.getGoldReward());
        }

        progressRepository.save(progress);
    }

    /**
     * Award gold reward to user
     */
    private void awardGoldReward(User user, Quest quest, UserQuestProgress progress) {
        if (progress.getRewardClaimed()) {
            return;
        }

        user.setGold(user.getGold().add(BigDecimal.valueOf(quest.getGoldReward())));
        userRepository.save(user);
        
        progress.setRewardClaimed(true);
        progress.setRewardClaimedAt(LocalDateTime.now());
    }

    /**
     * Admin: Reset weekly quests for a specific user
     */
    @Transactional
    public void resetWeeklyQuestsForUser(UUID userId) {
        String currentWeek = getCurrentWeekKey();
        progressRepository.resetWeeklyQuestsForUser(userId, currentWeek);
        log.info("Reset weekly quests for user {} (week {})", userId, currentWeek);
    }

    /**
     * Admin: Reset all weekly quests for all users
     */
    @Transactional
    public void resetAllWeeklyQuests() {
        String currentWeek = getCurrentWeekKey();
        progressRepository.resetAllWeeklyQuests(currentWeek);
        log.info("Reset all weekly quests for week {}", currentWeek);
    }

    /**
     * Convert progress to DTO
     */
    private UserQuestProgressDto toDto(UserQuestProgress progress) {
        Quest quest = progress.getQuest();
        double percent = quest.getRequiredAmount() > 0 
            ? (double) progress.getCurrentAmount() / quest.getRequiredAmount() * 100 
            : 0;

        return UserQuestProgressDto.builder()
            .progressId(progress.getId())
            .questId(quest.getId())
            .code(quest.getCode())
            .title(quest.getTitle())
            .description(quest.getDescription())
            .questType(quest.getQuestType().name())
            .goldReward(quest.getGoldReward())
            .currentAmount(progress.getCurrentAmount())
            .requiredAmount(quest.getRequiredAmount())
            .isCompleted(progress.getIsCompleted())
            .rewardClaimed(progress.getRewardClaimed())
            .completedAt(progress.getCompletedAt())
            .progressPercent(Math.min(percent, 100))
            .build();
    }
}

