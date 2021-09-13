package com.art.meetbot.domain.repository;

import com.art.meetbot.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Arthur Kupriyanov on 20.11.2020
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTelegramId(String telegramId);
}
