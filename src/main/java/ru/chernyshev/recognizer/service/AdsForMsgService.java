package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.entity.AdsInMsg;
import ru.chernyshev.recognizer.entity.AdsInMsgShown;
import ru.chernyshev.recognizer.repository.AdsForMsgRepository;
import ru.chernyshev.recognizer.repository.AdsForMsgShownRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class AdsForMsgService {

    private static final Logger logger = LoggerFactory.getLogger(AdsForMsgService.class);
    private static final long DEFAULT_ADS = -1L;

    private final AdsForMsgRepository adsRepository;
    private final AdsForMsgShownRepository adsForMsgShownRepository;

    public AdsInMsg adsInMsg;

    @Autowired
    public AdsForMsgService(AdsForMsgRepository adsRepository, AdsForMsgShownRepository adsForMsgShownRepository) {
        this.adsRepository = adsRepository;
        this.adsForMsgShownRepository = adsForMsgShownRepository;
    }

    @Scheduled(fixedDelay = 60 * 1000)
    public synchronized void checkAds() {
        LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/Moscow"));
        Optional<AdsInMsg> ads = adsRepository.findByDay(currentDate);
        if (ads.isPresent()) {
            adsInMsg = ads.get();
            return;
        }
        ads = adsRepository.findById(DEFAULT_ADS);
        adsInMsg = ads.orElse(null);
    }

    public synchronized AdsInMsg getAdsForMsg() {
        return adsInMsg;
    }

    public void saveAdsSendInfo(Long adsId, Long chatId, Long messageId) {
        AdsInMsgShown adsDirectSend = new AdsInMsgShown(adsId, chatId, messageId);
        adsForMsgShownRepository.save(adsDirectSend);
    }
}
