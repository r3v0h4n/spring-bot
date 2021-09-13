package com.art.meetbot.handler.profile;

import com.art.meetbot.bot.handle.Handler;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.domain.entity.CommandReg;
import com.art.meetbot.domain.repository.CommandRegRepository;
import com.art.meetbot.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

@Handler("/createprofile")
@Component
@Slf4j
public class CreateProfileCommand implements RequestHandler {
    private final CommandRegRepository commandRegRepository;
    private final UserRepository userRepository;

    public CreateProfileCommand(CommandRegRepository commandRegRepository, UserRepository userRepository) {
        this.commandRegRepository = commandRegRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BotApiMethod<Message> execute(Message message) {
        log.info("Start creating profile for user with chatId" + message.getChatId());
        CommandReg commandReg = commandRegRepository.findByChatId(message.getChatId())
                .orElse(new CommandReg(message.getChatId()));

        commandReg.setState(0);
        commandReg.setSeqName("create-profile-seq");
        commandRegRepository.save(commandReg);

        // remove old user data
        userRepository.findByTelegramId(String.valueOf(message.getChatId()))
                .ifPresent(userRepository::delete);

        return MessageUtils.sendText("Let's start creating a profile. \nAnswer a series of questions. \n\nRemember that this data will be seen by other users \n Old profile deleted \n " +
                                     "\nEnter your name:", message);
    }
}
