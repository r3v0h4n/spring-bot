package com.art.meetbot.handler.test;

import com.art.meetbot.bot.handle.Handler;
import com.art.meetbot.bot.handle.IgnoreActive;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.domain.entity.CommandReg;
import com.art.meetbot.domain.repository.CommandRegRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

@Handler("/test")
@Component
@Slf4j
@RequiredArgsConstructor
public class TestCommand implements RequestHandler {
    private final CommandRegRepository commandRegRepository;

    @Override
    public BotApiMethod<Message> execute(Message message) {
        log.info("Start creating profile for user with chatId" + message.getChatId());
        CommandReg commandReg = commandRegRepository.findByChatId(message.getChatId())
                .orElse(new CommandReg(message.getChatId()));

        commandReg.setState(0);
        commandReg.setSeqName("test-seq");
        commandRegRepository.save(commandReg);

        return MessageUtils.sendText("Тест на далбайоба", message);
    }
}
