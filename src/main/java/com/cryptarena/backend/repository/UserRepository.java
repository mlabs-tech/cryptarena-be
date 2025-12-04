package com.cryptarena.backend.repository;

import com.cryptarena.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTwitterId(String twitterId);

    Optional<User> findByTwitterUsername(String twitterUsername);

    boolean existsByTwitterId(String twitterId);

    boolean existsByTwitterUsername(String twitterUsername);
}

