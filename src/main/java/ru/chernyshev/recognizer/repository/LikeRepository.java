package ru.chernyshev.recognizer.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.chernyshev.recognizer.entity.LikeEntity;

public interface LikeRepository extends CrudRepository<LikeEntity, Long> {

    @Query(value = "select count(*) from LIKES le where le.message_id = :messageId and le.rating = :rating", nativeQuery = true)
    Integer count(Long messageId, Integer rating);
}
