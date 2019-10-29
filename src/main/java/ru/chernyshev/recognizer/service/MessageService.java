package ru.chernyshev.recognizer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Voice;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.repository.MessageRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageEntity create(ChatEntity chat, Voice voice, Integer messageId) {
        MessageEntity message = new MessageEntity();
        message.setChat(chat);
        message.setTelegramId(messageId);
        message.setDuration(voice.getDuration());
        return messageRepository.save(message);
    }

    public void update(MessageEntity message, MessageResult error) {
        message.setMessageType(MessageType.VOICE);
        message.setResult(error);
        messageRepository.save(message);
    }

    public void addLike(MessageEntity message, UserEntity from) {
        LikeEntity like = new LikeEntity();
        like.setRating(3);
        like.setUser(from);
        like.setMessage(message);
        message.getLikes().add(like);
        messageRepository.save(message);
    }

    public void updateSuccess(MessageEntity message, RecognizerType recognizerType, Integer messageId) {
        message.setMessageType(MessageType.VOICE);
        message.setResult(MessageResult.SUCCESS);
        message.setRecognizerType(recognizerType);
        message.setTelegramId(messageId);
        messageRepository.save(message);
    }
}
