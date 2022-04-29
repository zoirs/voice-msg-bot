package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.entity.AdsEntity;
import ru.chernyshev.recognizer.model.AdsButton;
import ru.chernyshev.recognizer.model.AdsType;
import ru.chernyshev.recognizer.repository.AdsDirectRepository;
import ru.chernyshev.recognizer.repository.AdsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdsService {

    private static final Logger logger = LoggerFactory.getLogger(AdsService.class);

    private final AdsRepository adsRepository;
    private final AdsDirectRepository adsDirectRepository;

    private AdsButton adsButton;

    @Autowired
    public AdsService(AdsRepository adsRepository, AdsDirectRepository adsDirectRepository) {
        this.adsRepository = adsRepository;
        this.adsDirectRepository = adsDirectRepository;
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000)
    public synchronized void checkTask() {
        logger.info("Start finding ads");
        LocalDateTime now = LocalDateTime.now();
        List<AdsEntity> currentTasks = adsRepository.findByStartBeforeAndFinishAfter(now, now);
        if (currentTasks.isEmpty()) {
            logger.info("No active ads tasks");
            return;
        }
        if (currentTasks.size() > 1) {
            String ids = currentTasks.stream()
                    .map(AdsEntity::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            logger.error("More than one active ads: {}", ids);
            return;
        }
        AdsEntity adsEntity = currentTasks.get(0);
        if (adsEntity.getType() == AdsType.DIRECT) {
            long wasSendCount = adsDirectRepository.countByAdsId(adsEntity.getId());
            if (wasSendCount >= adsEntity.getMaxCount()) {
                logger.info("Already sended max count {} for : {}", adsEntity.getMaxCount(), adsEntity.getId());
                return;
            }
        }
        adsButton = new AdsButton(adsEntity);
        logger.info("Find active ads : {}", adsButton);
    }

    AdsButton getCurrent() {
        if (adsButton == null || adsButton.isExpiry()) {
            return null;
        }
        return adsButton;
    }
}
