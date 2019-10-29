package ru.chernyshev.recognizer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.LikeResult;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.repository.LikeRepository;
import ru.chernyshev.recognizer.repository.MessageRepository;

@Service
public class MessageRatingService {

    private final LikeRepository likeRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    @Autowired
    public MessageRatingService(LikeRepository likeRepository, MessageRepository messageRepository, UserService userService) {
        this.likeRepository = likeRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    public void addLike(MessageEntity message, UserEntity from, Integer rating) {
        LikeEntity like = new LikeEntity();
        like.setRating(rating);
        like.setUser(from);
        like.setMessage(message);
        likeRepository.save(like);
    }

    public LikeResult addLike(Integer messageId, User user, Integer rating) {
        LikeEntity likeEntity = likeRepository.findByUserTelegramIdAndMessageTelegramId(user.getId(), messageId);
        if (likeEntity != null) {
            likeEntity.setRating(rating);
            likeRepository.save(likeEntity);
            return LikeResult.LIKE_UPDATED;
        }
        UserEntity userEntity = userService.getOrCreate(user);
        MessageEntity messageEntity = messageRepository.findByTelegramId(messageId);
        if (messageEntity == null) {
            return LikeResult.LIKE_NONE;
        }
        addLike(messageEntity, userEntity, rating);
        return LikeResult.LIKE_ADDED;
    }
}
