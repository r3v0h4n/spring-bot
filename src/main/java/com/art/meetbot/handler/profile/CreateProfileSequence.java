package com.art.meetbot.handler.profile;

import com.art.meetbot.bot.handle.Sequence;
import com.art.meetbot.bot.handle.SequenceHandler;
import com.art.meetbot.bot.util.KeyboardFactory;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.domain.entity.CommandReg;
import com.art.meetbot.domain.repository.CommandRegRepository;
import com.art.meetbot.domain.repository.UserRepository;
import com.art.meetbot.domain.entity.enums.Sex;
import com.art.meetbot.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Sequence("create-profile-seq")
@Component
public class CreateProfileSequence implements SequenceHandler {
    private final CommandRegRepository commandRegRepository;
    private final UserRepository userRepository;

    public CreateProfileSequence(CommandRegRepository commandRegRepository, UserRepository userRepository) {
        this.commandRegRepository = commandRegRepository;
        this.userRepository = userRepository;
    }

    // A lot of duplicate code at this method
    // TODO delete duplicate code
    @Override
    public BotApiMethod<? extends BotApiObject> handleCommand(Message message, int state) {
        log.debug("Received command " + message.getText() + " with state : " + state);

        CommandReg commandReg = commandRegRepository.findByChatId(message.getChatId())
                .orElseGet(() -> {
                    log.warn("Command reg is not found for chat " + message.getChatId());
                    return new CommandReg();
                });

        User user = userRepository.findByTelegramId(message.getChatId().toString())
                .orElse(new User(String.valueOf(message.getChatId())));

        switch (state) {
            case 0 -> {
                user.getUserInfo().setName(message.getText());
                changeState(1, commandReg);
                userRepository.save(user);
                return MessageUtils.sendText("Your birth date: ", message);
            }
            case 1 -> {

                try {
                    user.getUserInfo().setBirthYear(Integer.parseInt(message.getText()));
                    changeState(2, commandReg);
                } catch (NumberFormatException ignored) {
                    log.debug("user send not a year of birth with id " + message.getChatId());
                    return MessageUtils.sendText("Please, input a number!", message);
                }

                userRepository.save(user);
                return SendMessage.builder()
                        .text("Select gender")
                        .chatId(String.valueOf(message.getChatId()))
                        .replyMarkup(KeyboardFactory.selectGender())
                        .build();
            }
            case 2 -> {
                changeState(3, commandReg);
                user.getUserInfo().setSex(Sex.getGender(message.getText()));

                userRepository.save(user);
                return MessageUtils.sendText("Describe yourself: (who you are, what do you like and etc)", message);
            }
            case 3 -> {
                user.getUserInfo().setDescription(message.getText());
                userRepository.save(user);
                changeState(4, commandReg);
                return SendMessage.builder()
                        .text("Add your profile photo." +
                              "\nIf you don't want to add photo - you can answer \"no\"")
                        .chatId(String.valueOf(message.getChatId()))
                        .replyMarkup(KeyboardFactory.no())
                        .build();
            }

            case 4 -> {
                if ("no".equals(message.getText())) {
                    commandRegRepository.delete(commandReg);
                    return MessageUtils.sendText("Your profile ready without photo. You can add it later", message);
                }

                if (message.hasPhoto()) {
                    log.debug("Message has a photo");
                    receivedPhoto(message, user);
                    userRepository.save(user);
                    commandRegRepository.delete(commandReg);
                    return MessageUtils.sendText("Successfully created profile", message);
                } else {
                    return SendMessage.builder()
                            .text("Fail to read your photo. Please, try again")
                            .chatId(String.valueOf(message.getChatId()))
                            .replyMarkup(KeyboardFactory.no())  // it's important to add no button
                            .build();
                }

            }

            default -> commandRegRepository.delete(commandReg);
        }

        log.warn("something strange");
        commandRegRepository.delete(commandReg);
        return SendMessage.builder()
                .text("Command not found")
                .chatId(String.valueOf(message.getChatId()))
                .build();
    }

    private void receivedPhoto(Message message, User user) {
        List<PhotoSize> photos = message.getPhoto();
        // Get largest photo's file_id
        String photo_id = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElseThrow().getFileId();

        log.debug("Upload photo to database");

        user.getUserInfo().setPhotoId(photo_id);
        userRepository.save(user);
    }

    private void changeState(int newState, CommandReg commandReg) {
        commandReg.setState(newState);
        commandRegRepository.save(commandReg);
    }

}
