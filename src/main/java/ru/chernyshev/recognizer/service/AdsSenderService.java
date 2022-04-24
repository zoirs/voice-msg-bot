package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.entity.AdsDirectEntity;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.AdsButton;
import ru.chernyshev.recognizer.model.AdsType;
import ru.chernyshev.recognizer.repository.AdsDirectRepository;
import ru.chernyshev.recognizer.repository.ChatRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AdsSenderService {
    private static final Logger logger = LoggerFactory.getLogger(AdsSenderService.class);

    private final Pageable firstPage;
    private final RecognizerBotService recognizerBotService;
    private final AdsService adsService;
    private final AdsDirectRepository adsDirectRepository;
    private final ChatRepository chatRepository;

    @Autowired
    public AdsSenderService(RecognizerBotService recognizerBotService,
                            AdsService adsService,
                            AdsDirectRepository adsDirectRepository,
                            ChatRepository chatRepository,
                            @Value("${ads.batch.size}") int batchSize) {
        this.recognizerBotService = recognizerBotService;
        this.adsService = adsService;
        this.adsDirectRepository = adsDirectRepository;
        this.chatRepository = chatRepository;
        this.firstPage = PageRequest.of(0, batchSize, Sort.by("id"));
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000)
    public void checkNeedSend() {
        logger.info("Start ADS sending job");

        adsService.checkTask();
        AdsButton current = adsService.getCurrent();
        if (current == null || current.getType() != AdsType.DIRECT) {
            logger.info("No ads task. ADS send complete");
            return;
        }

        logger.info("Start sending for task {}", current);

        if (current.getTestChatId() != null) {
            logger.info("Send to test chat {}", current.getTestChatId());
            Optional<ChatEntity> chatEntity = chatRepository.findById(current.getTestChatId());
            if (!chatEntity.isPresent()) {
                logger.warn("Error test chat id {}", current.getTestChatId());
                return;
            }
            recognizerBotService.sendDirectMessage(current, chatEntity.get().getTelegramId());
            AdsDirectEntity adsDirectEntity = new AdsDirectEntity(current.getId(), current.getTestChatId());
            adsDirectRepository.save(adsDirectEntity);
            current.inc();
            return;
        }

        Optional<AdsDirectEntity> firstForThisAds = Optional.empty();
        AdsDirectEntity lastAdsSend = adsDirectRepository.findFirstByOrderByIdDesc();
        Long lastChatId = Optional.ofNullable(lastAdsSend).map(AdsDirectEntity::getChatId).orElse(0L);
        while (!current.isExpiry()) {
            List<ChatEntity> batchChats = chatRepository.findAllByGroupTypeAndIdGreaterThan("private", lastChatId, firstPage);
            if (batchChats.isEmpty()) {
                logger.info("All chats was send, restart ads list");
                lastChatId = 0L;
                firstForThisAds = Optional.ofNullable(adsDirectRepository.findFirstByAdsIdOrderByIdAsc(current.getId()));
                continue;
            }

            logger.info("Start send batch from {} (telegramId={}) to {} (telegramId={})", batchChats.get(0).getId(), batchChats.get(0).getTelegramId(), batchChats.get(batchChats.size() - 1).getId(), batchChats.get(batchChats.size() - 1).getTelegramId());
            for (ChatEntity chatEntity : batchChats) {
                if (firstForThisAds.map(q -> q.getChatId().equals(chatEntity.getId())).orElse(false)) {
                    logger.info("Ads was send to all chats, break sending");
                    return;
                }
                logger.info("Send to chat {}, telegramId {}", chatEntity.getId(), chatEntity.getTelegramId());
                recognizerBotService.sendDirectMessage(current, chatEntity.getTelegramId());
                AdsDirectEntity adsDirectSend = new AdsDirectEntity(current.getId(), chatEntity.getId());
                adsDirectRepository.save(adsDirectSend);
                current.inc();
                lastChatId = adsDirectSend.getChatId();
                if (current.isExpiry()) {
                    break;
                }
            }
        }
        logger.info("Complete sending for task {}", current);
    }
}
