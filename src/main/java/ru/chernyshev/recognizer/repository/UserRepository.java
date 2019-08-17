package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.User;

public interface UserRepository extends CrudRepository<User, Long> {
    User findByChatId(Long chatId);
}
