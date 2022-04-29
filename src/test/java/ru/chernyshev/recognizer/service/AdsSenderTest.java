package ru.chernyshev.recognizer.service;

import com.google.common.collect.Streams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import ru.chernyshev.recognizer.FixtureHelper;
import ru.chernyshev.recognizer.RepoFactory4Test;
import ru.chernyshev.recognizer.entity.AdsDirectEntity;
import ru.chernyshev.recognizer.entity.AdsEntity;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.model.AdsButton;
import ru.chernyshev.recognizer.model.AdsType;
import ru.chernyshev.recognizer.repository.AdsDirectRepository;
import ru.chernyshev.recognizer.repository.AdsRepository;
import ru.chernyshev.recognizer.repository.ChatRepository;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AdsSenderTest.AdsSenderTestTestContextConfiguration.class, RepoFactory4Test.class})
@AutoConfigureMockMvc
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class})
public class AdsSenderTest {

    private static final Logger logger = LoggerFactory.getLogger(AdsSenderTest.class);

    @Autowired
    AdsSenderService adsSenderService;
    @Autowired
    AdsService adsService;
    @Autowired
    AdsRepository adsRepository;
    @Autowired
    AdsDirectRepository adsDirectRepository;
    @Autowired
    ChatRepository chatRepository;

    @Configuration
    @TestPropertySource("classpath:application.properties")
    static class AdsSenderTestTestContextConfiguration {

        @Value("${ads.batch.size}")
        private int batchSize;
        @Value("${ads.direct.max.count.per.second}")
        private int maxMessagePerSecond;

        @MockBean
        RecognizerBotService recognizerBotService;

        @Bean
        public AdsService adsService(AdsRepository adsRepository, AdsDirectRepository adsDirectRepository) {
            return new AdsService(adsRepository, adsDirectRepository);
        }

        @Bean
        public AdsSenderService adsSenderService(AdsDirectRepository adsDirectRepository, AdsService adsService, ChatRepository chatRepository) {
            return new AdsSenderService(recognizerBotService, adsService, adsDirectRepository, chatRepository, batchSize, maxMessagePerSecond);
        }
    }

    @Before
    public void before() {
        logger.info("Before test start");
        chatRepository.deleteAll();
        adsRepository.deleteAll();
        adsDirectRepository.deleteAll();
        logger.info("db is clear");
    }

    @Test
    public void addTaskCheck() {
        AdsEntity s = createAdsTask(13L);
        adsRepository.save(s);

        adsService.checkTask();
        AdsButton current = adsService.getCurrent();
        Assert.assertNotNull(current);
    }

    @Test
    public void sendTaskCheck() {
        Iterable<AdsDirectEntity> wasSend = adsDirectRepository.findAll();
        Assert.assertFalse(wasSend.iterator().hasNext());

        AdsEntity ads = createAdsTask(13L);
        adsRepository.save(ads);

        List<ChatEntity> chats = generateChats(100, null);
        chatRepository.saveAll(chats);

        adsSenderService.checkNeedSend();
        wasSend = adsDirectRepository.findAll();

        Set<Long> sendToTelegramIds = new HashSet<>();
        Iterator<AdsDirectEntity> wasSendIterator = wasSend.iterator();
        for (int i = 0; i < ads.getMaxCount(); i++) {
            Assert.assertTrue(wasSendIterator.hasNext());
            AdsDirectEntity adsSend = wasSendIterator.next();
            Assert.assertFalse(sendToTelegramIds.contains(adsSend.getChatId()));
            sendToTelegramIds.add(adsSend.getChatId());
        }
        Assert.assertFalse(wasSendIterator.hasNext());
    }

    @Test
    public void notSendAdsTwice() {
        Iterable<AdsDirectEntity> wasSend = adsDirectRepository.findAll();
        Assert.assertFalse(wasSend.iterator().hasNext());

        AdsEntity ads = createAdsTask(13L);
        adsRepository.save(ads);

        List<ChatEntity> chats = generateChats(7, "private");
        chatRepository.saveAll(chats);

        Assert.assertTrue(ads.getMaxCount() > chats.size());

        adsSenderService.checkNeedSend();
        wasSend = adsDirectRepository.findAll();

        Iterator<AdsDirectEntity> wasSendIterator = wasSend.iterator();
        for (int i = 0; i < chats.size(); i++) {
            Assert.assertTrue(wasSendIterator.hasNext());
            wasSendIterator.next();
        }
        Assert.assertFalse(wasSendIterator.hasNext());
    }

    @Test
    public void notSendAdsTwiceFromCenter() {
        Iterable<AdsDirectEntity> wasSend = adsDirectRepository.findAll();
        Assert.assertFalse(wasSend.iterator().hasNext());

        AdsEntity ads = createAdsTask(13L);
        adsRepository.save(ads);

        List<ChatEntity> chats = generateChats(7, "private");
        chatRepository.saveAll(chats);

        AdsEntity completeAdsTask = createCompleteAdsTask();
        adsRepository.save(completeAdsTask);

        long completeTaskId = completeAdsTask.getId();
        adsDirectRepository.save(new AdsDirectEntity(completeTaskId, chats.get(0).getId(), true));
        adsDirectRepository.save(new AdsDirectEntity(completeTaskId, chats.get(1).getId(), true));

        Assert.assertTrue(ads.getMaxCount() > chats.size());

        adsSenderService.checkNeedSend();

        Iterable<AdsDirectEntity> allSend = adsDirectRepository.findAll();
        Assert.assertEquals(2, Streams.stream(allSend).filter(q -> q.getAdsId().equals(completeTaskId)).count());
        Assert.assertEquals(7, Streams.stream(allSend).filter(q -> q.getAdsId().equals(ads.getId())).count());

    }

    @Test
    public void checkLimitRate() {
        AdsEntity ads = createAdsTask(120L);
        adsRepository.save(ads);

        List<ChatEntity> chats = generateChats(120, "private");
        chatRepository.saveAll(chats);

        LocalDateTime before = LocalDateTime.now();
        adsSenderService.checkNeedSend();
        long seconds = before.until(LocalDateTime.now(), ChronoUnit.SECONDS );
        logger.debug("test check limit rate: {}", seconds);
        Assert.assertTrue(seconds > 20);
    }

    private List<ChatEntity> generateChats(int count, @Nullable String groupType) {
        List<ChatEntity> chats = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chats.add(FixtureHelper.createChat(i, groupType));
        }
        return chats;
    }

    private AdsEntity createAdsTask(Long maxCount) {
        return new AdsEntity(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1), "ya.ru", "some", null, maxCount, AdsType.DIRECT);
    }

    private AdsEntity createCompleteAdsTask() {
        return new AdsEntity(LocalDateTime.now().minusMinutes(2), LocalDateTime.now().minusMinutes(1), "ya.ru", "some", null, 13L, AdsType.DIRECT);
    }
}
