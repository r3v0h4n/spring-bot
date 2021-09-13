package com.art.meetbot.handler.profile;

import com.art.meetbot.bot.handle.Handler;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.util.KeyboardFactory;
import com.art.meetbot.domain.entity.CommandReg;
import com.art.meetbot.domain.repository.CommandRegRepository;
import com.art.meetbot.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Handler("/search")
@Component
@Slf4j
public class SearchProfileCommand implements RequestHandler {
    private final CommandRegRepository commandRegRepository;

    public SearchProfileCommand(CommandRegRepository commandRegRepository, UserRepository userRepository) {
        this.commandRegRepository = commandRegRepository;
    }

    @Override
    public BotApiMethod<Message> execute(Message message) {
        log.info("Start searching for user with chatId" + message.getChatId());
        CommandReg commandReg = commandRegRepository.findByChatId(message.getChatId())
                .orElse(new CommandReg(message.getChatId()));

        commandReg.setState(0);
        commandReg.setSeqName("searching-profiles-seq");
        commandRegRepository.save(commandReg);
        return SendMessage.builder()
                .text("Let's start searching")
                .chatId(String.valueOf(message.getChatId()))
                .replyMarkup(KeyboardFactory.yesNo())
                .build();
    }
}
