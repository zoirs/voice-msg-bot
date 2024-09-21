package ru.chernyshev.recognizer.service;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.chernyshev.recognizer.entity.AdsSended;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.AdsButton;
import ru.chernyshev.recognizer.model.AdsType;
import ru.chernyshev.recognizer.repository.ChatRepository;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Service
public class AdsSenderService {
    private static final String LINK_PATTERN = "$link$";

    private static final Logger logger = LoggerFactory.getLogger(AdsSenderService.class);

    private final Pageable firstPage;
    private final RecognizerBotService recognizerBotService;
    private final AdsService adsService;
    private final ChatRepository chatRepository;
    private final RateLimiter rateLimiter;

    @Autowired
    public AdsSenderService(RecognizerBotService recognizerBotService,
                            AdsService adsService,
                            ChatRepository chatRepository,
                            @Value("${ads.batch.size}") int batchSize,
                            @Value("${ads.direct.max.count.per.second}") long maxContMessagePerSecond) {
        this.recognizerBotService = recognizerBotService;
        this.adsService = adsService;
        this.chatRepository = chatRepository;
        this.firstPage = PageRequest.of(0, batchSize, Sort.by("id"));
        this.rateLimiter = RateLimiter.create(maxContMessagePerSecond);
    }

    @Scheduled(fixedDelay = 1 * 60 * 1000)
    public void checkNeedSend() {
        logger.info("Start ADS sending job");

        adsService.checkTask();
        AdsButton current = adsService.getCurrent();
        if (current == null || current.getType() != AdsType.DIRECT) {
            logger.info("No active ads DIRECT task.");
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
            SendPhoto sendPhoto = prepareAdsMessage(current, chatEntity.get().getTelegramId());
            boolean result = recognizerBotService.sendDirect(sendPhoto);
            adsService.saveAdsSendInfo(current.getId(), current.getTestChatId(), null, result);
            current.inc();
            return;
        }

        Optional<AdsSended> firstForThisAds = Optional.empty();
        AdsSended lastAdsSend = adsService.getLastSended();
        Long lastChatId = Optional.ofNullable(lastAdsSend).map(AdsSended::getChatId).orElse(0L);
        while (!current.isExpiry()) {
            List<ChatEntity> batchChats = chatRepository.findAllByGroupTypeAndIdGreaterThan("private", lastChatId, firstPage);
            if (batchChats.isEmpty()) {
                logger.info("All chats was send, restart ads list");
                lastChatId = 0L;
                firstForThisAds = Optional.ofNullable(adsService.getFirstForAds(current.getId()));
                continue;
            }

            logger.info("Start send batch from {} (telegramId={}) to {} (telegramId={})", batchChats.get(0).getId(), batchChats.get(0).getTelegramId(), batchChats.get(batchChats.size() - 1).getId(), batchChats.get(batchChats.size() - 1).getTelegramId());
            for (ChatEntity chatEntity : batchChats) {
                if (firstForThisAds.map(q -> q.getChatId().equals(chatEntity.getId())).orElse(false)) {
                    logger.info("Ads was send to all chats, break sending");
                    return;
                }
                SendPhoto sendPhoto = prepareAdsMessage(current, chatEntity.getTelegramId());
                rateLimiter.acquire();
                boolean result = recognizerBotService.sendDirect(sendPhoto);
                logger.info("Send to chatID {} (telegramId {}) success = {}; For ads {} send {}", chatEntity.getId(), chatEntity.getTelegramId(), result, current.getId(), current.getCurrentCount());
                adsService.saveAdsSendInfo(current.getId(), chatEntity.getId(), null, result);
                current.inc();
                lastChatId = chatEntity.getId();
                if (current.isExpiry()) {
                    break;
                }
            }
        }
        logger.info("Complete sending for task {}", current);
    }

    private SendPhoto prepareAdsMessage(AdsButton adsEntity, Long telegramChatId) {
        if (adsEntity == null || adsEntity.getType() != AdsType.DIRECT || adsEntity.getFilePath() == null) {
            return null;
        }
        if (adsEntity.getUrl() == null) {
            logger.warn("Ads link is empty");
            return null;
        }
        if (!adsEntity.getText().contains(LINK_PATTERN)) {
            logger.warn("Ads text no places for ads link");
            return null;
        }
        File image = new File(adsEntity.getFilePath());
        if (!Files.exists(image.toPath())) {
            logger.warn("File {} not exist", image);
            return null;
        }
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(image));

        sendPhoto.setChatId(String.valueOf(telegramChatId));// 268305576

        sendPhoto.setParseMode("MarkdownV2");
        String encodedUrl = adsEntity.getUrl().replaceAll("\\.", "\\\\.");
        String text = adsEntity.getText()
                .replaceAll("\\.", "\\\\.")
                .replace(LINK_PATTERN, encodedUrl);
        sendPhoto.setCaption(text);
        return sendPhoto;
    }
}
