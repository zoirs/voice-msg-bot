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

@SpringBootApplication
public class RecognizerApplication {

	private static Logger logger = LoggerFactory.getLogger(RecognizerApplication.class);

	public static void main(String[] args) {
		logger.info("Start app...");
		ApiContextInitializer.init();
		SpringApplication.run(RecognizerApplication.class, args);
		logger.info("app init");
	}

	@Bean
	TelegramBotsApi telegramBotsApi(RecognizerBot recognizerBot) throws TelegramApiRequestException {
		logger.info("Start bot init");
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
		telegramBotsApi.registerBot(recognizerBot);
		logger.info("Bot registered");
		return telegramBotsApi;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
