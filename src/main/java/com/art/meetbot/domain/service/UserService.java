package com.art.meetbot.domain.service;

import com.art.meetbot.domain.entity.User;
import com.art.meetbot.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String chatId) {
        return userRepository.findByTelegramId(chatId).orElse(new User(chatId));

    }
}
