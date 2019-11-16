package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.LikeResult;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.repository.LikeRepository;
import ru.chernyshev.recognizer.repository.MessageRepository;

@Service
public class MessageRatingService {

    private static final Logger logger = LoggerFactory.getLogger(MessageRatingService.class);

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
    public LikeResult addLike(Message message, User user, int rating) {
        logger.info("Add like message {} in chat {} ", message.getMessageId(), message.getChatId());
        MessageEntity messageEntity = messageRepository.findByTelegramIdAndChat_ChatId(message.getMessageId(), message.getChatId());
        if (messageEntity == null) {
            return LikeResult.LIKE_NONE;
        }
        UserEntity userEntity = userService.getOrCreate(user);

        LikeEntity likeEntity = messageEntity.getLikes().stream()
                .filter(l -> l.getUser().getTelegramId().equals(user.getId()))
                .findFirst().orElse(null);

        if (likeEntity != null) {
            logger.info("MessageId {}, by {}, update rating {}", messageEntity.getId(), messageEntity.getRecognizerType(), rating);
            if (likeEntity.getRating() != rating) {
                likeEntity.setRating(rating);
                likeRepository.save(likeEntity);
            }
            return LikeResult.LIKE_UPDATED;
        }

        logger.info("MessageId {}, by {}, set rating {}", messageEntity.getId(), messageEntity.getRecognizerType(), rating);

        LikeEntity like = new LikeEntity();
        like.setRating(rating);
        like.setUser(userEntity);
        like.setMessage(messageEntity);
        likeRepository.save(like);
        return LikeResult.LIKE_ADDED;
    }
}
