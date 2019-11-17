package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.ChatEntity;

public interface ChatRepository extends CrudRepository<ChatEntity, Long> {
    ChatEntity findByTelegramId(Long chatId);
}
