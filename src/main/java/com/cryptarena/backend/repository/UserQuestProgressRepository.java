package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.Quest;
import com.cryptarena.backend.entity.User;
import com.cryptarena.backend.entity.UserQuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, UUID> {

    List<UserQuestProgress> findByUser(User user);

    List<UserQuestProgress> findByUserId(UUID userId);

    Optional<UserQuestProgress> findByUserAndQuest(User user, Quest quest);

    Optional<UserQuestProgress> findByUserIdAndQuestId(UUID userId, UUID questId);

    Optional<UserQuestProgress> findByUserAndQuestCode(User user, String questCode);

    /**
     * Find progress for weekly quests for a specific week
     */
    Optional<UserQuestProgress> findByUserAndQuestAndWeekKey(User user, Quest quest, String weekKey);

    /**
     * Find all weekly quest progress for a specific week
     */
    List<UserQuestProgress> findByUserAndWeekKey(User user, String weekKey);

    /**
     * Reset weekly quests for a user (delete progress for a specific week)
     */
    @Modifying
    @Query("DELETE FROM UserQuestProgress p WHERE p.user.id = :userId AND p.quest.questType = 'WEEKLY' AND p.weekKey = :weekKey")
    void resetWeeklyQuestsForUser(@Param("userId") UUID userId, @Param("weekKey") String weekKey);

    /**
     * Reset all weekly quests for all users for a specific week
     */
    @Modifying
    @Query("DELETE FROM UserQuestProgress p WHERE p.quest.questType = 'WEEKLY' AND p.weekKey = :weekKey")
    void resetAllWeeklyQuests(@Param("weekKey") String weekKey);
}

