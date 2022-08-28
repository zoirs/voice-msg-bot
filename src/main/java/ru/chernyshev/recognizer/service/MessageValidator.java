package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Voice;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.ChatStatus;
import ru.chernyshev.recognizer.model.MessageResult;

import java.time.Instant;
import java.util.Date;

@Service
public class MessageValidator {
    private static final int MAX_SIZE = 10 * 1024 * 1024;
    public static final int MAX_SECONDS = 180;

    private static final Logger logger = LoggerFactory.getLogger(MessageValidator.class);

    private final ChatService chatService;

    @Autowired
    public MessageValidator(ChatService chatService) {
        this.chatService = chatService;
    }

    private MessageResult validate(ChatEntity chat, Voice voice, Integer sendDate){

        if (!"audio/ogg".equals(voice.getMimeType())) {
            logger.info("Incorrect audio format {}", voice.getMimeType());
            return MessageResult.VOICE_MSG_INCORRECT_FORMAT;
        }

        if (chat.getState() != ChatStatus.ACTIVE) {
            logger.info("Chat {} is {}", chat.getId(), chat.getState());
            return MessageResult.BANNED;
        }

//        Long messageToday = chatService.getMessagesToday(chat);
//        if (messageToday > MAX_MESSAGES_PER_DAY) {
//            logger.info("Chat {} send {} today", chat.getId(), messageToday);
//            return MessageResult.VOICE_MSG_TOO_MUCH_TODAY;
//        }

        Integer duration = voice.getDuration();
        if (duration > MAX_SECONDS) {
            logger.info("Message too long: {}", duration);
            return MessageResult.VOICE_MSG_TOO_LONG;
        }

        long unixTimestamp = Instant.now().getEpochSecond();
        long delaySeconds = unixTimestamp - sendDate;
        if (delaySeconds > 30) {
            logger.info("Message OVERDUE! Delay is {}. Was send at {}", delaySeconds, new Date(sendDate * 1000));
            return MessageResult.OVERDUE;
        }

        if (voice.getFileSize() > MAX_SIZE) {
            logger.warn("Message too big: {}", voice.getFileSize());
            return MessageResult.VOICE_MSG_TOO_HARD;
        }

        return null;
    }

    public MessageResult validate(Message receivedMsg) {
        ChatEntity chatEntity = chatService.getOrCreate(receivedMsg.getChat());
        return validate(chatEntity, receivedMsg.getVoice(), receivedMsg.getDate());
    }
}
