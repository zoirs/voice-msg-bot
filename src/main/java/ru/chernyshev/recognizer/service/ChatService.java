package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.ChatStatus;
import ru.chernyshev.recognizer.repository.MessageRepository;
import ru.chernyshev.recognizer.repository.ChatRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private static final String USERCHATTYPE = "private";
    private static final String GROUPCHATTYPE = "group";
    private static final String CHANNELCHATTYPE = "channel";
    private static final String SUPERGROUPCHATTYPE  = "supergroup";

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    public ChatEntity getOrCreate(Chat chat) {
        ChatEntity chatEntity = chatRepository.findByChatId(chat.getId());
        if (chatEntity == null) {
            chatEntity = new ChatEntity();
            chatEntity.setChatId(chat.getId());
            chatEntity.setUserName(chat.getUserName());
            chatEntity.setFirstName(chat.getFirstName());
            chatEntity.setLastName(chat.getLastName());
            chatEntity.setGroupType(getGroupType(chat));
            chatEntity.setState(ChatStatus.ACTIVE);
            chatEntity.setGroupName(chat.getTitle());
            chatRepository.save(chatEntity);
            logger.info("Create new chatEntity by chat {}", chat.toString());
        } else {
            // Миграция для старых данных
            if (!StringUtils.isEmpty(chat.getTitle()) && StringUtils.isEmpty(chatEntity.getGroupName())) {
                chatEntity.setGroupName(chat.getTitle());
                chatRepository.save(chatEntity);
                logger.info("ChatEntity was updated chat {}", chat.toString());
            }
        }
        return chatEntity;
    }

    public Long getMessagesToday(ChatEntity chat) {
        return messageRepository.countToday(startOfDay(), endOfDay(), MessageResult.SUCCESS, chat);
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
