package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {

    Optional<Quest> findByCode(String code);

    List<Quest> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<Quest> findByQuestTypeAndIsActiveTrue(Quest.QuestType questType);
}

