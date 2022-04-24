package ru.chernyshev.recognizer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.ChatEntity;

import java.util.List;

public interface ChatRepository extends CrudRepository<ChatEntity, Long> {
    ChatEntity findByTelegramId(Long chatId);
    List<ChatEntity> findAllByGroupTypeAndIdGreaterThan(String group, Long id, Pageable pageable);
}