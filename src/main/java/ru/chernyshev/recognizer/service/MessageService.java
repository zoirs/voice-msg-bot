package ru.chernyshev.recognizer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.entity.Message;
import ru.chernyshev.recognizer.entity.User;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.repository.MessageRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message create(User user) {
        Message message = new Message();
        message.setUser(user);
        return messageRepository.save(message);
    }

    public void update(Message message, MessageType voice, MessageResult error) {
        message.setMessageType(voice);
        message.setResult(error);
        messageRepository.save(message);

    }
}
