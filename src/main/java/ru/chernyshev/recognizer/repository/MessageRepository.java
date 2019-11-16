package ru.chernyshev.recognizer.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.model.MessageResult;

import java.util.Date;
import java.util.List;

public interface MessageRepository extends CrudRepository<MessageEntity, Long> {

    @Query("select count(m) from MessageEntity m where m.created >= :startDate and m.created <= :endDate and m.result = :result and m.chat = :chat")
    Long countToday(@Param("startDate") Date startDate,
                    @Param("endDate") Date endDate,
                    @Param("result") MessageResult result,
                    @Param("chat") ChatEntity chat);

    MessageEntity findByTelegramIdAndChat_ChatId(Integer telegramId, Long chatId);
}
