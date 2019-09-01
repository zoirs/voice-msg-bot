package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.ChatEntity;

public interface UserRepository extends CrudRepository<ChatEntity, Long> {
    ChatEntity findByChatId(Long chatId);
}
