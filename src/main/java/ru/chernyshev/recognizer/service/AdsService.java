package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.chernyshev.recognizer.entity.AdsSended;
import ru.chernyshev.recognizer.entity.AdsEntity;
import ru.chernyshev.recognizer.model.AdsButton;
import ru.chernyshev.recognizer.model.AdsType;
import ru.chernyshev.recognizer.repository.AdsSendedRepository;
import ru.chernyshev.recognizer.repository.AdsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdsService {

    private static final Logger logger = LoggerFactory.getLogger(AdsService.class);

    private final AdsRepository adsRepository;
    private final AdsSendedRepository adsDirectRepository;

    private AdsButton adsButton;

    @Autowired
    public AdsService(AdsRepository adsRepository, AdsSendedRepository adsDirectRepository) {
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
        long wasSendCount = 0;
        if (adsEntity.getType() == AdsType.DIRECT) {
            wasSendCount = adsDirectRepository.countByAdsId(adsEntity.getId());
            if (wasSendCount >= adsEntity.getMaxCount()) {
                logger.info("Already sended {} ;max count {} for : {}", wasSendCount, adsEntity.getMaxCount(), adsEntity.getId());
                return;
            }
        }
        adsButton = new AdsButton(adsEntity, wasSendCount);
        logger.info("Find active ads : {}", adsButton);
    }

    public void saveAdsSendInfo(Long adsId, Long chatId, Long messageId, boolean result){
        AdsSended adsDirectSend = new AdsSended(adsId, chatId, messageId, result);
        adsDirectRepository.save(adsDirectSend);
    }

    public AdsSended getLastSended() {
        List<AdsSended> result = adsDirectRepository.getLastDirectSended(PageRequest.of(0, 1));
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        return result.get(0);
    }

    public AdsSended getFirstForAds(Long adsId) {
        return adsDirectRepository.findFirstByAdsIdOrderByIdAsc(adsId);
    }

    AdsButton getCurrent() {
        if (adsButton == null || adsButton.isExpiry()) {
            return null;
        }
        return adsButton;
    }

    public Set<Long> getResultFor(Long ads_id, Set<Long> chatIds) {
        return adsDirectRepository.getResult(ads_id, chatIds).stream().map(AdsSended::getChatId).collect(Collectors.toSet());
    }
}
