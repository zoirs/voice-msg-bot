package ru.chernyshev.recognizer.repository;

import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsSended;

public interface AdsDirectRepository extends CrudRepository<AdsSended, Long> {
    AdsSended findFirstByOrderByIdDesc();
    AdsSended findFirstByAdsIdOrderByIdAsc(Long adsId);
    Long countByAdsId(Long adsId);
}
