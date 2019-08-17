package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.Message;

public interface MessageRepository extends CrudRepository<Message, Long> {
}
