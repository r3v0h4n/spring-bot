package com.art.meetbot.bot.core;

import com.art.meetbot.bot.core.loader.SequenceLoader;
import com.art.meetbot.bot.handle.IgnoreActive;
import com.art.meetbot.bot.handle.MessageHandler;
import com.art.meetbot.bot.handle.RequestHandler;
import com.art.meetbot.bot.handle.SequenceHandler;
import com.art.meetbot.bot.util.MessageUtils;
import com.art.meetbot.domain.entity.CommandReg;
import com.art.meetbot.domain.entity.User;
import com.art.meetbot.domain.repository.CommandRegRepository;
import com.art.meetbot.domain.repository.UserRepository;
import com.art.meetbot.domain.service.UserService;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class SequenceService {
    private static final Map<String, SequenceHandler> seqMap = new HashMap<>();
    private final SequenceLoader sequenceLoader;
    private final CommandRegRepository commandRegRepository;
    private final UserService userService;

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
        // if user have sequence in db
        if (commandRegOptional.isPresent()) {
            // found sequence handler
            Optional<SequenceHandler> sequenceHandlerOptional = resolveSequenceHandler(commandRegOptional.get());
            if (sequenceHandlerOptional.isPresent()) {
                // get result of sequence handler
                result = getResult(sequenceHandlerOptional.get(), message, commandRegOptional.get().getState());
            }
        }
        return Optional.ofNullable(result);
    }

    private Optional<SequenceHandler> resolveSequenceHandler(CommandReg commandReg) {
            // if sequence handler exists
            if (seqMap.containsKey(commandReg.getSeqName())) {
                return Optional.of(seqMap.get(commandReg.getSeqName()));
            }
            return Optional.empty();
    }

    private BotApiMethod<? extends BotApiObject> getResult(SequenceHandler sequenceHandler, Message message, int state) throws InvocationTargetException, IllegalAccessException {
        User user = userService.getOrCreateUser(message.getChatId().toString());
        Class<?> sequenceClass = sequenceHandler.getClass();
        if (user.isActive() || sequenceClass.isAnnotationPresent(IgnoreActive.class)) {
            return findMethod(sequenceHandler, message, state)
                    .orElse(sequenceHandler.handleCommand(message, state));
        }
        else {
            return MessageUtils.sendUserNotActive(message);
        }
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
