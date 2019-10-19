package ru.chernyshev.recognizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.chernyshev.recognizer.service.RecognizerBotService;

@SpringBootApplication
public class RecognizerApplication {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerApplication.class);

    public static void main(String[] args) {
        logger.info("Start app...");
        ApiContextInitializer.init();
        SpringApplication.run(RecognizerApplication.class, args);
        logger.info("app init");
    }

    @Bean
    TelegramBotsApi telegramBotsApi(RecognizerBotService recognizerBotService) throws TelegramApiRequestException {
        logger.info("Start bot init");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        telegramBotsApi.registerBot(recognizerBotService);
        logger.info("Bot registered");
        return telegramBotsApi;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new ErrorHandler());
        return restTemplate;
    }
}
