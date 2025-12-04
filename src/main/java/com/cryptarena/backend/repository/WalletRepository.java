package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByAddress(String address);

    Optional<Wallet> findByAddressIgnoreCase(String address);

    List<Wallet> findByUserId(UUID userId);

    List<Wallet> findByUserIdAndWalletType(UUID userId, String walletType);

    boolean existsByAddress(String address);

    boolean existsByAddressIgnoreCase(String address);

    Optional<Wallet> findByUserIdAndIsPrimaryTrueAndWalletType(UUID userId, String walletType);

    int countByUserId(UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}

