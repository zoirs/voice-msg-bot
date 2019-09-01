package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.UserStatus;
import ru.chernyshev.recognizer.repository.MessageRepository;
import ru.chernyshev.recognizer.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class ChatService {
    private static Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX = 50;

    private static final String USERCHATTYPE = "private";
    private static final String GROUPCHATTYPE = "group";
    private static final String CHANNELCHATTYPE = "channel";
    private static final String SUPERGROUPCHATTYPE  = "supergroup";

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public ChatEntity getOrCreate(Chat chat) {
        ChatEntity user = userRepository.findByChatId(chat.getId());
        if (user == null) {
            user = new ChatEntity();
            user.setChatId(chat.getId());
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setGroupType(getGroupType(chat));
            user.setState(UserStatus.ACTIVE);
            userRepository.save(user);
            logger.info("Create new user by chat {}", chat.toString());
        }
        return user;
    }

    public boolean isValid(ChatEntity chat) {
        Long messageToday = messageRepository.countToday(startOfDay(), endOfDay(), MessageResult.SUCCESS, chat);
        if (messageToday > MAX) {
            logger.info("Chat {} send {} today", chat.getId(), messageToday);
            return false;
        }
        if (chat.getState() != UserStatus.ACTIVE) {
            logger.info("Chat {} is {}", chat.getId(), chat.getState());
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

    private String getGroupType(Chat chat) {
        if (chat.isChannelChat()) {
            return CHANNELCHATTYPE;
        }
        if (chat.isGroupChat()) {
            return GROUPCHATTYPE;
        }
        if (chat.isSuperGroupChat()) {
            return SUPERGROUPCHATTYPE;
        }
        if (chat.isUserChat()) {
            return USERCHATTYPE;
        }
        return null;
    }
}
