package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.chernyshev.recognizer.entity.ChatAdsRequestEntity;
import ru.chernyshev.recognizer.model.ChatStatus;
import ru.chernyshev.recognizer.repository.ChatAdsRequestRepository;

@Service
public class ChatAdsRequestService {
    private static final Logger logger = LoggerFactory.getLogger(ChatAdsRequestService.class);

    private final ChatAdsRequestRepository chatRepository;

    public ChatAdsRequestService(ChatAdsRequestRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public ChatAdsRequestEntity getOrCreate(Chat chat) {
        ChatAdsRequestEntity chatEntity = chatRepository.findByTelegramId(chat.getId());
        if (chatEntity == null) {
            chatEntity = new ChatAdsRequestEntity();
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
            if (chatEntity.isRemoved()) {
                logger.info("Chat come back {}", chat);
                chatEntity.setRemoved(false);
                chatRepository.save(chatEntity);
            }
        }
        return chatEntity;
    }

    public void remove(ChatAdsRequestEntity chatEntity) {
        chatEntity.setRemoved(true);
        chatRepository.save(chatEntity);
    }

    private String getGroupType(Chat chat) {
        if (chat.isChannelChat()) {
            return ChatService.CHANNELCHATTYPE;
        }
        if (chat.isGroupChat()) {
            return ChatService.GROUPCHATTYPE;
        }
        if (chat.isSuperGroupChat()) {
            return ChatService.SUPERGROUPCHATTYPE;
        }
        if (chat.isUserChat()) {
            return ChatService.USERCHATTYPE;
        }
        return null;
    }
}
