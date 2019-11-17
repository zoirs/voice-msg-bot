package ru.chernyshev.recognizer.service;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.repository.LikeRepository;
import ru.chernyshev.recognizer.repository.MessageRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class MessageRatingService {

    private static final Logger logger = LoggerFactory.getLogger(MessageRatingService.class);
    private static final long MIN_MILLIS_FOR_UPDATE = 1_000;

    private final LikeRepository likeRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    @Autowired
    public MessageRatingService(LikeRepository likeRepository, MessageRepository messageRepository, UserService userService) {
        this.likeRepository = likeRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @Transactional
    public LikeEntity addLike(Message message, User user, int rating) {
        MessageEntity messageEntity = messageRepository.findByTelegramIdAndChat_TelegramId(message.getMessageId(), message.getChatId());
        if (messageEntity == null) {
            return null;
        }
        UserEntity userEntity = userService.getOrCreate(user);

        LikeEntity likeEntity = messageEntity.getLikes().stream()
                .filter(l -> l.getUser().getTelegramId().equals(user.getId()))
                .findFirst().orElse(null);

        if (likeEntity != null) {
            if (canUpdate(likeEntity, rating)) {
                logger.info("MessageId {}, by {}, update rating {}", messageEntity.getId(), messageEntity.getRecognizerType(), rating);
                likeEntity.setRating(rating);
                likeEntity = likeRepository.save(likeEntity);
                logRating(messageEntity);
                return likeEntity;
            } else {
                return null;
            }
        }

        logger.info("MessageId {}, by {}, set rating {}", messageEntity.getId(), messageEntity.getRecognizerType(), rating);

        LikeEntity like = new LikeEntity();
        like.setRating(rating);
        like.setUser(userEntity);
        like.setMessage(messageEntity);
        likeRepository.save(like);
        logRating(messageEntity);
        return like;
    }

    public Map<String, Integer> getRating(MessageEntity message) {
        Integer disCount = likeRepository.count(message.getId(), -1);
        Integer likeCount = likeRepository.count(message.getId(), 1);
        return ImmutableMap.of("dis", disCount, "like", likeCount);
    }

    private boolean canUpdate(LikeEntity likeEntity, int rating) {
        if (likeEntity.getRating() == rating) {
            return false;
        }
        if (likeEntity.getUpdated() == null) {
            return true;
        }
        long secondsFromLastUpdate = likeEntity.getUpdated().until(LocalDateTime.now(), ChronoUnit.MILLIS);
        if (secondsFromLastUpdate < MIN_MILLIS_FOR_UPDATE) {
            logger.warn("MessageId {} update rating too fast", likeEntity.getMessage().getId());
            return false;
        }
        return true;
    }

    private void logRating(MessageEntity message) {
        Integer disCount = likeRepository.count(message.getId(), -1);
        Integer likeCount = likeRepository.count(message.getId(), 1);
        logger.info("Statistic for message {}, {}. like: {} dis: {}", message.getId(), message.getRecognizerType(), likeCount, disCount);
    }
}
