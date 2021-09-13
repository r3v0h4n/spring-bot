package com.art.meetbot.handler.profile;

import com.art.meetbot.bot.MeetBot;
import com.art.meetbot.bot.handle.Sequence;
import com.art.meetbot.bot.handle.SequenceHandler;
import com.art.meetbot.bot.util.KeyboardFactory;
import com.art.meetbot.domain.entity.CommandReg;
import com.art.meetbot.domain.repository.CommandRegRepository;
import com.art.meetbot.domain.repository.MatchedPeopleRepository;
import com.art.meetbot.domain.repository.UserRepository;
import com.art.meetbot.domain.entity.MatchedPeople;
import com.art.meetbot.domain.entity.User;
import com.art.meetbot.domain.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Sequence("searching-profiles-seq")
@Component
public class SearchProfilesSequence implements SequenceHandler {
    private final CommandRegRepository commandRegRepository;
    private final UserRepository userRepository;
    private final MatchedPeopleRepository matchedPeopleRepository;

    public SearchProfilesSequence(CommandRegRepository commandRegRepository, UserRepository userRepository, MatchedPeopleRepository matchedPeopleRepository) {
        this.commandRegRepository = commandRegRepository;
        this.userRepository = userRepository;
        this.matchedPeopleRepository = matchedPeopleRepository;
    }

    @Override
    public BotApiMethod<? extends BotApiObject> handleCommand(Message message, int state) {
        log.debug("Received command " + message.getText() + " with state : " + state);

        CommandReg commandReg = commandRegRepository.findByChatId(message.getChatId())
                .orElseGet(() -> {
                    log.warn("Command reg is not found for chat " + message.getChatId());
                    return new CommandReg();
                });

        List<User> users = userRepository.findAll();

        if (message.getText().equals("accept")) {
            if (state != 0) {
                acceptUser(message.getChatId(), users.get(state - 1));
            } else {
                log.warn("user send accept on 1 question");
            }
        }
        if (message.getText().equals("no") || userRepository.count() <= state) {
            commandRegRepository.delete(commandReg);
            return SendMessage.builder()
                    .text("search finished\nFounded:" + (userRepository.count() - 1) + " profile")
                    .chatId(String.valueOf(message.getChatId()))
                    .build();
        }
        if (users.get(state).getTelegramId().equals(message.getChatId().toString())) {
            state += 1;
            if (state >= users.size()) {
                commandRegRepository.delete(commandReg);
                return SendMessage.builder()
                        .text("search finished\nFounded:" + (users.size() - 1) + " profile")
                        .chatId(String.valueOf(message.getChatId()))
                        .build();
            }
        }

        changeState(state + 1, commandReg);

        UserInfo userInfo = users.get(state).getUserInfo();

        sendPhoto(message.getChatId(), userInfo.getPhotoId());
        String responseText = makeMessageFromUserInfo(userInfo);

        return SendMessage.builder()
                .text(responseText)
                .chatId(String.valueOf(message.getChatId()))
                .replyMarkup(KeyboardFactory.nextAccept())
                .build();

    }

    private void changeState(int newState, CommandReg commandReg) {
        commandReg.setState(newState);
        commandRegRepository.save(commandReg);
    }

    private void acceptUser(Long chatId, User user) {
        boolean isMatched = matchedPeopleRepository.findAll().stream()
                .anyMatch(entity -> entity.getTelegramIdSecond().equals(chatId.toString())
                                    && entity.getTelegramIdFirst().equals(user.getTelegramId()));

        if (isMatched) {
            String reciprocity = "You have reciprocity!\n";
            String message = makeMessageFromUserInfoWithName(
                    userRepository.findByTelegramId(chatId.toString()).get().getUserInfo());
            SendMessage sendMessage = SendMessage.builder()
                    .text(reciprocity + message)
                    .chatId(user.getTelegramId())
                    .build();

            String messageToCurrent = makeMessageFromUserInfoWithName(user.getUserInfo());
            SendMessage sendMessageToCurrent = SendMessage.builder()
                    .text(reciprocity + messageToCurrent)
                    .chatId(chatId.toString())
                    .build();
            try {
                MeetBot.instance.execute(sendMessage);
                MeetBot.instance.execute(sendMessageToCurrent);
            } catch (TelegramApiException e) {
                log.warn("Can't send message to user with chat id" + user.getTelegramId());
            }
        } else {
            MatchedPeople matchedPeople = new MatchedPeople();
            matchedPeople.setTelegramIdFirst(chatId.toString());
            matchedPeople.setTelegramIdSecond(user.getTelegramId());
            matchedPeopleRepository.save(matchedPeople);
        }
    }

    private String makeMessageFromUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            return "";
        }
        String description = userInfo.getDescription() == null ? "" : userInfo.getDescription();

        return "Next user. \nGender:" + userInfo.getSex() +
               "\nBirth year:" + userInfo.getBirthYear() +
               "\nDescription:" + description;
    }

    private String makeMessageFromUserInfoWithName(UserInfo userInfo) {
        if (userInfo == null) {
            return "";
        }

        String message = makeMessageFromUserInfo(userInfo);
        String description = userInfo.getName() == null ? "" : userInfo.getName();

        return message + "\nName:" + userInfo.getName();
    }

    private void sendPhoto(Long chatId, String photoId) {
        if (photoId == null) {
            log.debug("Photo not found for user");
            return;
        }

        SendPhoto msg = SendPhoto.builder()
                .chatId(chatId.toString())
                .photo(new InputFile(photoId))
                .caption("Photo")
                .build();
        try {
            MeetBot.instance.execute(msg); // Call method to send the photo
        } catch (TelegramApiException e) {
            log.warn("Can't send message to user with chat id" + chatId);
        }
    }
}
