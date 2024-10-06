package ru.chernyshev.recognizer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.chernyshev.recognizer.entity.AdsSended;

import java.util.List;
import java.util.Set;

public interface AdsDirectRepository extends CrudRepository<AdsSended, Long> {

    @Query(value = "select s from AdsSended s where s.adsId not in (select id from AdsEntity where type != 'DIRECT' or testChatId is not null) order by id desc")
    List<AdsSended> getLastDirectSended(Pageable pageable);
    @Query(value = "select s from AdsSended s where s.adsId = :adsID and chatId in :chatIds")
    List<AdsSended> getResult(@Param("adsID")Long adsId, @Param("chatIds") Set<Long> chatIds);
    AdsSended findFirstByAdsIdOrderByIdAsc(Long adsId);
    Long countByAdsId(Long adsId);
}
