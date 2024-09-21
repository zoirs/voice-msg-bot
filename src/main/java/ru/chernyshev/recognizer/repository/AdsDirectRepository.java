package ru.chernyshev.recognizer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.AdsSended;

import java.util.List;

public interface AdsDirectRepository extends CrudRepository<AdsSended, Long> {

    @Query(value = "select s from AdsSended s where s.adsId not in (select id from AdsEntity where type != 'DIRECT' or testChatId is not null) order by id desc")
    List<AdsSended> getLastDirectSended(Pageable pageable);
    AdsSended findFirstByAdsIdOrderByIdAsc(Long adsId);
    Long countByAdsId(Long adsId);
}
