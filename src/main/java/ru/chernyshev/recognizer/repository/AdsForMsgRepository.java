package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsInMsg;

import java.time.LocalDate;
import java.util.Optional;

public interface AdsForMsgRepository extends CrudRepository<AdsInMsg, Long> {
    Optional<AdsInMsg> findByDay(LocalDate localDate);
}
