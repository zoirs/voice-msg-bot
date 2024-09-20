package ru.chernyshev.recognizer;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.chernyshev.recognizer.service.RecognizerBotService;

@ConditionalOnProperty(value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@SpringBootApplication
public class RecognizerApplication {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerApplication.class);

    public static void main(String[] args) {
        logger.info("Start app...");
        SpringApplication.run(RecognizerApplication.class, args);
        logger.info("app init");
    }

    @Bean
    TelegramBotsApi telegramBotsApi(RecognizerBotService recognizerBotService) throws TelegramApiException {
        logger.info("Start bot init");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(recognizerBotService);
        logger.info("Bot registered");
        return telegramBotsApi;
    }

    @Bean
    public RestTemplate restTemplate(@Value("${rest.connection.timeout}") int connectionTimeout,
                                     @Value("${rest.read.timeout}") int readTimeout,
                                     @Value("${rest.connect.timeout}") int connectTimeout) {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(40)
                .setMaxConnPerRoute(4)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectionRequestTimeout(connectionTimeout);
        requestFactory.setReadTimeout(readTimeout);
        requestFactory.setConnectTimeout(connectTimeout);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(new ErrorHandler());
        return restTemplate;
    }

    @Bean
    public MessageSource messageSource(){
        ResourceBundleMessageSource resource = new ResourceBundleMessageSource();
        resource.setBasename("messages");
        return resource;
    }
}
