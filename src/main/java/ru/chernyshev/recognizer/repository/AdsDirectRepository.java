package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsDirectEntity;

public interface AdsDirectRepository extends CrudRepository<AdsDirectEntity, Long> {
    AdsDirectEntity findFirstByOrderByIdDesc();
    AdsDirectEntity findFirstByAdsIdOrderByIdAsc(Long adsId);
    Long countByAdsId(Long adsId);
}
