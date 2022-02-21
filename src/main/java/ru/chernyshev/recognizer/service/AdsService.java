package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.entity.AdsEntity;
import ru.chernyshev.recognizer.model.AdsButton;
import ru.chernyshev.recognizer.repository.AdsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdsService {

    private static final Logger logger = LoggerFactory.getLogger(AdsService.class);

    private final AdsRepository adsRepository;

    private AdsButton adsButton;

    @Autowired
    public AdsService(AdsRepository adsRepository) {
        this.adsRepository = adsRepository;
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000)
    public void scheduleFixedDelayTask() {
        logger.info("Start read ads");
        LocalDateTime now = LocalDateTime.now();
        List<AdsEntity> currentTasks = adsRepository.findByStartBeforeAndFinishAfter(now, now);
        if (currentTasks.isEmpty()) {
            return;
        }
        if (currentTasks.size() > 1) {
            logger.warn("More than one active ads");
        }
        AdsEntity adsEntity = currentTasks.get(0);
        adsButton = new AdsButton(adsEntity.getStart(), adsEntity.getFinish(), adsEntity.getUrl(), adsEntity.getText());
        logger.info("Active ads : {}", adsButton);
    }

    AdsButton getCurrent() {
        if (adsButton == null || adsButton.isExpiry()) {
            return null;
        }
        return adsButton;
    }
}
