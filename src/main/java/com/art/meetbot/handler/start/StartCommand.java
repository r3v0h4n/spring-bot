package com.art.meetbot.handler.start;

import com.art.meetbot.bot.handle.Handler;
import com.art.meetbot.bot.handle.IgnoreActive;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

@Handler("/start")
@Component
@Slf4j
@RequiredArgsConstructor
@IgnoreActive
public class StartCommand implements RequestHandler {
    @Override
    public BotApiMethod<Message> execute(Message message) {
        return MessageUtils.sendText("ХЕЙ", message);
    }
}
