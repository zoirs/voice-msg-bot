package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsInMsgShown;

public interface AdsForMsgShownRepository extends CrudRepository<AdsInMsgShown, Long> {
}
