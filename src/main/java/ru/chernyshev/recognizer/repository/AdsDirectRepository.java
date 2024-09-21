package ru.chernyshev.recognizer.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsSended;

public interface AdsDirectRepository extends CrudRepository<AdsSended, Long> {

    @Query(value = "select * from ads_sended where ads_id not in (select id from ads where type != 'DIRECT' or test_chat_id is not null) order by id desc limit 1", nativeQuery = true)
    AdsSended getLastDirectSended();
    AdsSended findFirstByAdsIdOrderByIdAsc(Long adsId);
    Long countByAdsId(Long adsId);
}
