package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.CryptoCoin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CryptoCoinRepository extends JpaRepository<CryptoCoin, UUID> {

    Optional<CryptoCoin> findBySymbol(String symbol);

    Optional<CryptoCoin> findByName(String name);

    boolean existsBySymbol(String symbol);
}

