package ru.chernyshev.recognizer.service;

import ru.chernyshev.recognizer.model.MessageResult;

import java.util.HashMap;
import java.util.Map;

import static ru.chernyshev.recognizer.model.MessageResult.*;
import static ru.chernyshev.recognizer.service.MessageValidator.MAX_MESSAGES_PER_DAY;
import static ru.chernyshev.recognizer.service.MessageValidator.MAX_SECONDS;

public class MsgUtils {
    private static final Map<MessageResult, String> MESSAGES = new HashMap<MessageResult, String>() {{
        put(VOICE_MSG_TOO_LONG, "Недоступны сообщения длительностью более " + MAX_SECONDS + " секунд");
        put(VOICE_MSG_TOO_HARD, "Недоступны объемные сообщения");
        put(VOICE_MSG_TOO_MUCH_TODAY, "Превышено ограничение " + MAX_MESSAGES_PER_DAY + " сообщение в день");
    }};

    public static String getNotification(MessageResult messageResult) {
        return MESSAGES.get(messageResult);
    }
}
