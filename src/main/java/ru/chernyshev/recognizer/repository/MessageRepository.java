package ru.chernyshev.recognizer.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.chernyshev.recognizer.entity.Message;
import ru.chernyshev.recognizer.entity.User;
import ru.chernyshev.recognizer.model.MessageResult;

import java.util.Date;

public interface MessageRepository extends CrudRepository<Message, Long> {

    @Query("select count(m) from Message m where m.created >= :startDate and m.created <= :endDate and m.result = :result and m.user = :user")
    Long countToday(@Param("startDate") Date startDate,
                    @Param("endDate") Date endDate,
                    @Param("result") MessageResult result,
                    @Param("user") User user);
}
