package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.Champion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChampionRepository extends JpaRepository<Champion, UUID> {

    Optional<Champion> findByName(String name);

    Optional<Champion> findByCryptoCoinId(UUID cryptoCoinId);

    boolean existsByName(String name);
}

