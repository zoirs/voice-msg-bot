package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface AdsRepository extends CrudRepository<AdsEntity, Long> {
    List<AdsEntity> findByStartBeforeAndFinishAfter(LocalDateTime finish, LocalDateTime start);
}
