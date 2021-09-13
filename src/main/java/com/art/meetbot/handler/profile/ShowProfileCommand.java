package com.art.meetbot.handler.profile;

import com.art.meetbot.bot.MeetBot;
import com.art.meetbot.bot.handle.Handler;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.domain.repository.UserRepository;
import com.art.meetbot.domain.entity.User;
import com.art.meetbot.domain.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

/**
 * @author Arthur Kupriyanov on 22.11.2020
 */
@Handler("/profile")
@Component
@Slf4j
public class ShowProfileCommand implements RequestHandler {

    private final UserRepository userRepository;

    public ShowProfileCommand(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public BotApiMethod<Message> execute(Message message) {
        Optional<User> byTelegramId = userRepository.findByTelegramId(String.valueOf(message.getChatId()));

        return byTelegramId.map(user -> {
            if (user.getUserInfo().getPhotoId() != null) {
                try {
                    MeetBot.instance.execute(SendPhoto.builder()
                            .photo(new InputFile(user.getUserInfo().getPhotoId()))
                            .chatId(String.valueOf(message.getChatId()))
                            .build());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }


            return SendMessage.builder()
                    .chatId(String.valueOf(message.getChatId()))
                    .text("Your profile:\n\n" + profileInfo(user))
                    .build();
        }).orElse(
                MessageUtils.sendText("You don't have a profile. Please, use command /createprofile", message)
        );

    }

    private String profileInfo(User user) {
        UserInfo userInfo = user.getUserInfo();
        String answer = "Name: " + userInfo.getName();
        answer += "\nBirth year: " + userInfo.getBirthYear();
        answer += "\nSex: " + userInfo.getSex().toString();
        answer += "\nDescription: " + userInfo.getDescription();

        return answer;

    }
}
