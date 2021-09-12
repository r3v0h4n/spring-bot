package com.art.meetbot.bot.core;

import com.art.meetbot.bot.core.loader.SequenceLoader;
import com.art.meetbot.bot.handle.MessageHandler;
import com.art.meetbot.bot.handle.SequenceHandler;
import com.art.meetbot.entity.register.CommandReg;
import com.art.meetbot.entity.repo.register.CommandRegRepository;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Arthur Kupriyanov on 21.11.2020
 */
@Service
@Slf4j
public class SequenceService {
    private static final Map<String, SequenceHandler> seqMap = new HashMap<>();
    private final SequenceLoader sequenceLoader;
    private final CommandRegRepository commandRegRepository;

    public SequenceService(SequenceLoader sequenceLoader, CommandRegRepository commandRegRepository) {
        this.sequenceLoader = sequenceLoader;
        this.commandRegRepository = commandRegRepository;
    }

    @PostConstruct
    void init() {
        seqMap.putAll(sequenceLoader.load());
    }

    /**
     * resolves handler if sequence for user was founded else returns empty optional
     * @param message from user
     * @return result of method handleCommand in sequence handler if method with MessageHandler annotation not found
     * @throws InvocationTargetException because fuck you
     * @throws IllegalAccessException because fuck you
     */
    public Optional<BotApiMethod<? extends BotApiObject>> handle(Message message) throws InvocationTargetException, IllegalAccessException {
        Optional<CommandReg> commandRegOptional = commandRegRepository.findByChatId(message.getChatId());
        BotApiMethod<? extends BotApiObject> result = null;
        if (commandRegOptional.isPresent()) {
            CommandReg commandReg = commandRegOptional.get();
            if (seqMap.containsKey(commandReg.getSeqName())) {
                SequenceHandler sequenceInstance = seqMap.get(commandReg.getSeqName());
                result = findMethod(sequenceInstance, message, commandReg.getState())
                        .orElse(sequenceInstance.handleCommand(message, commandReg.getState()));
            } else {
                log.warn("Sequence not found");
            }
        }
        return Optional.ofNullable(result);
    }

    /**
    * finds method in sequenceInstance class by MessageHandler annotation
    **/
    @SuppressWarnings("unchecked")
    private Optional<BotApiMethod<? extends BotApiObject>> findMethod(SequenceHandler sequenceInstance, Message message, int state) throws InvocationTargetException, IllegalAccessException {
        Class<?> sequenceClass = sequenceInstance.getClass();

        for (final Method method : sequenceClass.getDeclaredMethods()) {
            // if method have MessageHandler
            if (method.isAnnotationPresent(MessageHandler.class)) {
                MessageHandler annotationInstance = method.getAnnotation(MessageHandler.class);
                if (annotationInstance.value().equals(message.getText()) && annotationInstance.state() == state) {
                    log.debug("Sequence method handler. Method name: " + method.getName());
                    Object result = method.invoke(sequenceInstance, message, state);
                    return Optional.ofNullable((BotApiMethod<? extends BotApiObject>) result);
                }
            }
        }
        // if method not found
        log.debug("Sequence default handler");
        return Optional.empty();
    }
}
