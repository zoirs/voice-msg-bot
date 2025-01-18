package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.ChatAdsRequestEntity;

public interface ChatAdsRequestRepository extends CrudRepository<ChatAdsRequestEntity, Long> {
    ChatAdsRequestEntity findByTelegramId(Long chatId);
}