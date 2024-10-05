package ru.chernyshev.recognizer.service;

import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.repository.MessageRepository;

import javax.annotation.Nullable;
import java.util.Locale;

@Service
public class MessageService {

    private final ChatService chatService;
    private final UserService userService;
    private final MessageRepository messageRepository;

    @Autowired
    public MessageService(ChatService chatService, UserService userService, MessageRepository messageRepository) {
        this.chatService = chatService;
        this.userService = userService;
        this.messageRepository = messageRepository;
    }

    public Locale getLocale(MessageEntity message) {
        ChatEntity chatEntity = chatService.require(message.getChat().getId());
        return Locales.find(chatEntity.getLocale());
    }

    private MessageEntity create(ChatEntity chatEntity, UserEntity userEntity, Integer duration, Integer messageId, MessageType type) {
        MessageEntity message = new MessageEntity();
        message.setChat(chatEntity);
        message.setUser(userEntity);
        message.setTelegramId(messageId);
        message.setDuration(duration);
        message.setMessageType(type);
        return messageRepository.save(message);
    }

    public void update(MessageEntity message, MessageResult result, Integer telegramId) {
        message.setResult(result);
        if (telegramId != null) {
            message.setTelegramId(telegramId);
        }
        messageRepository.save(message);
    }

    public void updateSuccess(MessageEntity message, RecognizerType recognizerType, Integer telegramId) {
        message.setResult(MessageResult.SUCCESS);
        message.setRecognizerType(recognizerType);
        message.setTelegramId(telegramId);
        messageRepository.save(message);
    }

    public MessageEntity create(Message receivedMsg) {
        ChatEntity chatEntity = chatService.getOrCreate(receivedMsg.getChat());
        UserEntity userEntity = userService.getOrCreate(MoreObjects.firstNonNull(receivedMsg.getForwardFrom(), receivedMsg.getFrom()));
        return create(chatEntity, userEntity, getDuration(receivedMsg), receivedMsg.getMessageId(), getType(receivedMsg));
    }

    public static Integer getDuration(Message receivedMsg) {
        if (receivedMsg.hasVoice()) {
            return receivedMsg.getVoice().getDuration();
        }
        if (receivedMsg.hasVideoNote()) {
            return receivedMsg.getVideoNote().getDuration();
        }
        return -1;
    }

    public static long getFileSize(Message receivedMsg) {
        if (receivedMsg.hasVoice()) {
            return receivedMsg.getVoice().getFileSize();
        }
        if (receivedMsg.hasVideoNote()) {
            return receivedMsg.getVideoNote().getFileSize();
        }
        return -1;
    }

    @Nullable
    public static String getFileId(Message receivedMsg) {
        if (receivedMsg.hasVoice()) {
            return receivedMsg.getVoice().getFileId();
        }
        if (receivedMsg.hasVideoNote()) {
            return receivedMsg.getVideoNote().getFileId();
        }
        return null;
    }

    public static MessageType getType(Message receivedMsg) {
        if (receivedMsg.hasVoice()) {
            return MessageType.VOICE;
        }
        if (receivedMsg.hasVideoNote()) {
            return MessageType.VIDEO;
        }
        return MessageType.UNKNOWN;
    }
}
