package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.chernyshev.recognizer.entity.User;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.UserStatus;
import ru.chernyshev.recognizer.repository.MessageRepository;
import ru.chernyshev.recognizer.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class UserService {
    private static Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX = 50;

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public UserService(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public User getOrCreate(Chat chat) {
        User user = userRepository.findByChatId(chat.getId());
        if (user == null) {
            user = new User();
            user.setChatId(chat.getId());
            user.setName(chat.getUserName());
            user.setState(UserStatus.ACTIVE);
            userRepository.save(user);
            logger.info("Create new user by chat {}", chat.toString());
        }
        return user;
    }

    public boolean isValid(User user) {
        Long messageToday = messageRepository.countToday(startOfDay(), endOfDay(), MessageResult.SUCCESS, user);
        if (messageToday > MAX) {
            logger.info("User {} send {} today", user.getId(), messageToday);
            return false;
        }
        if (user.getState() != UserStatus.ACTIVE) {
            logger.info("User {} is {}", user.getId(), user.getState());
            return false;
        }
        return true;
    }

    private static Date startOfDay() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    private static Date endOfDay() {
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
