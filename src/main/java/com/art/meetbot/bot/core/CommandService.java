package com.art.meetbot.bot.core;

import com.art.meetbot.bot.core.loader.CommandLoader;
import com.art.meetbot.bot.core.loader.LogLoader;
import com.art.meetbot.bot.handle.ExecutionTime;
import com.art.meetbot.bot.handle.IgnoreActive;
import com.art.meetbot.bot.handle.Log;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.handle.RequestLogger;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.domain.entity.User;
import com.art.meetbot.domain.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Arthur Kupriyanov on 20.11.2020
 */
@Service
@Slf4j
public class CommandService {
    private static final Map<String, RequestHandler> commandsMap = new HashMap<>();
    private static final Map<String, Set<RequestLogger>> loggersMap = new HashMap<>();
    private final CommandLoader commandLoader;
    private final LogLoader logLoader;
    private final UserService userService;

    private CommandService(CommandLoader commandLoader, LogLoader logLoader, UserService userService) {
        this.commandLoader = commandLoader;
        this.logLoader = logLoader;
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        initCommands();
        initLoggers();
    }

    private void initCommands() {
        commandsMap.putAll(commandLoader.readCommands());
        log.info("Total commands : " + commandsMap.toString());
    }

    private void initLoggers() {
        loggersMap.putAll(logLoader.loadLoggers());
    }

    public BotApiMethod<Message> handle(Message message) {
        if (commandsMap.containsKey(message.getText())) {
            return getResult(message, commandsMap.get(message.getText()));
        } else {
            return MessageUtils.commandNotFound(message);
        }
    }

    private BotApiMethod<Message> getResult(Message message, RequestHandler requestHandler) {
        User user = userService.getOrCreateUser(message.getChatId().toString());
        Class<?> requestClass = requestHandler.getClass();
        if (user.isActive() || requestClass.isAnnotationPresent(IgnoreActive.class)) {
            return requestHandler.execute(message);
        }
        else {
            return MessageUtils.sendUserNotActive(message);
        }
    }

    public Set<RequestLogger> findLoggers(String message, ExecutionTime executionTime) {
        final Set<RequestLogger> matchedLoggers = new HashSet<>();
        for (Map.Entry<String, Set<RequestLogger>> entry : loggersMap.entrySet()) {
            for (RequestLogger logger : entry.getValue()) {

                if (containsExecutionTime(extractExecutionTimes(logger), executionTime) ) {
                    if (message.matches(entry.getKey()))
                        matchedLoggers.add(logger);
                }
            }

        }

        return matchedLoggers;
    }

    private static ExecutionTime[] extractExecutionTimes(RequestLogger logger) {
        return logger.getClass().getAnnotation(Log.class).executionTime();
    }

    private static boolean containsExecutionTime(ExecutionTime[] times, ExecutionTime executionTime) {
        for (ExecutionTime et : times) {
            if (et == executionTime) return true;
        }

        return false;
    }

}
