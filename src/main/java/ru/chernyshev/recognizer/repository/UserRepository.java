package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.UserEntity;

public interface UserRepository extends CrudRepository<UserEntity, Long> {
    UserEntity findByTelegramId(Long telegramId);
}
