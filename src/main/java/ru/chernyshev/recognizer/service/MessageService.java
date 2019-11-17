package ru.chernyshev.recognizer.service;

import com.google.common.base.MoreObjects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Voice;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.repository.MessageRepository;

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

    private MessageEntity create(ChatEntity chatEntity, UserEntity userEntity, Voice voice, Integer messageId) {
        MessageEntity message = new MessageEntity();
        message.setChat(chatEntity);
        message.setUser(userEntity);
        message.setTelegramId(messageId);
        message.setDuration(voice.getDuration());
        return messageRepository.save(message);
    }

    public void update(MessageEntity message, MessageResult result, Integer telegramId) {
        message.setMessageType(MessageType.VOICE);
        message.setResult(result);
        if (telegramId != null) {
            message.setTelegramId(telegramId);
        }
        messageRepository.save(message);
    }

    public void updateSuccess(MessageEntity message, RecognizerType recognizerType, Integer telegramId) {
        message.setMessageType(MessageType.VOICE);
        message.setResult(MessageResult.SUCCESS);
        message.setRecognizerType(recognizerType);
        message.setTelegramId(telegramId);
        messageRepository.save(message);
    }

    public MessageEntity create(Message receivedMsg) {
        ChatEntity chatEntity = chatService.getOrCreate(receivedMsg.getChat());
        UserEntity userEntity = userService.getOrCreate(MoreObjects.firstNonNull(receivedMsg.getForwardFrom(), receivedMsg.getFrom()));
        return create(chatEntity, userEntity, receivedMsg.getVoice(), receivedMsg.getMessageId());
    }
}
