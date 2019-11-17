package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.ChatStatus;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.repository.ChatRepository;
import ru.chernyshev.recognizer.repository.MessageRepository;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private static final String USERCHATTYPE = "private";
    private static final String GROUPCHATTYPE = "group";
    private static final String CHANNELCHATTYPE = "channel";
    private static final String SUPERGROUPCHATTYPE = "supergroup";

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    public ChatEntity getOrCreate(Chat chat) {
        ChatEntity chatEntity = chatRepository.findByTelegramId(chat.getId());
        if (chatEntity == null) {
            chatEntity = new ChatEntity();
            chatEntity.setTelegramId(chat.getId());
            chatEntity.setUserName(chat.getUserName());
            chatEntity.setFirstName(chat.getFirstName());
            chatEntity.setLastName(chat.getLastName());
            chatEntity.setGroupType(getGroupType(chat));
            chatEntity.setState(ChatStatus.ACTIVE);
            chatEntity.setGroupName(chat.getTitle());
            chatRepository.save(chatEntity);
            logger.info("Create new chatEntity by chat {}", chat.toString());
        } else {
            boolean needUpdate = false;

            // Миграция для старых данных
            if (!StringUtils.isEmpty(chat.getTitle()) && StringUtils.isEmpty(chatEntity.getGroupName())) {
                logger.info("ChatEntity was updated chat {}", chat.toString());
                chatEntity.setGroupName(chat.getTitle());
                needUpdate = true;
            }

            if (chatEntity.isRemoved()) {
                logger.info("Chat come back {}", chat);
                chatEntity.setRemoved(false);
                needUpdate = true;
            }

            if (needUpdate) {
                chatRepository.save(chatEntity);
            }
        }
        return chatEntity;
    }

    public void remove(ChatEntity chatEntity) {
        chatEntity.setRemoved(true);
        chatRepository.save(chatEntity);
    }

    public Long getMessagesToday(ChatEntity chat) {
        return messageRepository.countToday(startOfDay(), endOfDay(), MessageResult.SUCCESS, chat);
    }

    private static LocalDateTime startOfDay() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        return startOfDay;
    }

    private static LocalDateTime endOfDay() {
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        return endOfDay;
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

    @NotNull
    public ChatEntity require(Long id) {
        return chatRepository.findById(id).get();
    }
}
