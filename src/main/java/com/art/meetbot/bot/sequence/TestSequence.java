package com.art.meetbot.bot.sequence;

import com.art.meetbot.bot.handle.MessageHandler;
import com.art.meetbot.bot.handle.Sequence;
import com.art.meetbot.bot.handle.SequenceHandler;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.entity.register.CommandReg;
import com.art.meetbot.entity.repo.register.CommandRegRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

@Slf4j
@Sequence("test-seq")
@Component
@RequiredArgsConstructor
public class TestSequence implements SequenceHandler {
    private final CommandRegRepository commandRegRepository;

    @MessageHandler(value="aaa")
    public BotApiMethod<? extends BotApiObject> aaa(Message message, int state) {
        return MessageUtils.sendText("eeeeeeeeee", message);
    }

    @Override
    public BotApiMethod<? extends BotApiObject> handleCommand(Message message, int state) {
        CommandReg commandReg = commandRegRepository.findByChatId(message.getChatId())
                .orElseGet(() -> new CommandReg(message.getChatId()));

        if ("SOS".equals(message.getText())) {
            commandRegRepository.delete(commandReg);
            return MessageUtils.sendText("Тест пройдений, але ти далбайоб!", message);
        }
        return MessageUtils.sendText("Тест не пройдений! Ви далбайоб. Пробуйте ще", message);
    }
}
