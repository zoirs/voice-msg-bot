package ru.chernyshev.recognizer.repository;

import org.hibernate.annotations.NamedNativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.model.MessageResult;

import java.util.Date;

public interface LikeRepository extends CrudRepository<LikeEntity, Long> {
//    @Query(
//            value = "select * from LIKES le where le.user_id = :userId and le.message_id = :messageId",
//            nativeQuery = true) не подходит тк собственные id
//    LikeEntity find(@Param("userId") Integer userId, @Param("messageId") Integer messageId);

    LikeEntity findByUserTelegramIdAndMessageTelegramId(Integer userId, Integer messageId);

}
