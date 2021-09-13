package com.art.meetbot.domain.repository;

import com.art.meetbot.domain.entity.CommandReg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Arthur Kupriyanov on 21.11.2020
 */
public interface CommandRegRepository extends JpaRepository<CommandReg, UUID> {
    Optional<CommandReg> findByChatId(Long chatId);
}
