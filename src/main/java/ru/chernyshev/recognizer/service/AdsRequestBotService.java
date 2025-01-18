package ru.chernyshev.recognizer.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.entity.ChatAdsRequestEntity;

import java.io.File;
import java.nio.file.Files;

@Service
public class AdsRequestBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(AdsRequestBotService.class);

    private final String botToken;
    private final String botUsername;
    private final String adsExamplePath;
    private final ChatAdsRequestService chatAdsRequestService;

    public AdsRequestBotService(@Value("${botads.token}") String botToken,
                                @Value("${botads.username}") String botUsername,
                                @Value("${botads.img.path}") String adsExamplePath,
                                ChatAdsRequestService chatAdsRequestService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatAdsRequestService = chatAdsRequestService;
        this.adsExamplePath = adsExamplePath;
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.info("ads bot");
        Message receivedMsg = update.getMessage();
//        if ("kicked".equals(update.getMyChatMember().getNewChatMember().getStatus())) {
//            todo удалили чат
//            return;
//        }
        if (receivedMsg == null) { // Обычно, это редактирование сообщения
            return;
        }

        if (receivedMsg.getEntities() != null
                && receivedMsg.getEntities().stream().anyMatch(q -> "bot_command".equalsIgnoreCase(q.getType()) && "/start".equalsIgnoreCase(q.getText()))) {
            logger.info("New user/chat add ads request bot");
            User from = receivedMsg.getFrom();
            if (from != null) {
                if (receivedMsg.getChat().isUserChat()) {
                    SendMessage sendMsg = new SendMessage();
                    sendMsg.setText("Реклама в @VoiceMsgBot \n" +
                            "(бот для расшифровки голосовых сообщений)\n" +
                            "\n" +
                            "Цена: 8000 рублей за сутки размещения, примерно 60-70 тыс. показов.\n" +
                            "\n" +
                            "Реклама добавляется к каждой расшифровке голосового сообщения. Сообщение будет видно всем участникам чата.\n" +
                            "\n" +
                            "Требования: \n" +
                            " • Только текст, без медиа, максимум 130 символов\n" +
                            " • Не рекламируем то что не законно\n" +
                            "\n" +
                            "Для заказа пришлите текст рекламного сообщения менеджеру: @VoiceMsgAds");
                    sendMsg.setChatId(receivedMsg.getChatId());
                    sendDirect(sendMsg);
                    sendDirect(prepareAdsExampleMessage(receivedMsg.getChatId()));
                }
            }
            Chat chat = receivedMsg.getChat();
            if (chat != null) {
                logger.info("New chat add ads request bot {}", chat);
                chatAdsRequestService.getOrCreate(chat);
            }
        }

        if (receivedMsg.getLeftChatMember() != null && receivedMsg.getLeftChatMember().getUserName().equals(getBotUsername())) {
            logger.info("Was removed {}", receivedMsg.getChat());
            ChatAdsRequestEntity chat = chatAdsRequestService.getOrCreate(receivedMsg.getChat());
            chatAdsRequestService.remove(chat);
        }
    }

    private SendPhoto prepareAdsExampleMessage(Long telegramChatId) {
        if (StringUtils.isEmpty(adsExamplePath)) {
            logger.warn("Ads example empty");
            return null;
        }
        File image = new File(adsExamplePath);
        if (!Files.exists(image.toPath())) {
            logger.warn("File {} not exist", image);
            return null;
        }
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(image));

        sendPhoto.setChatId(String.valueOf(telegramChatId));
        return sendPhoto;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    public boolean sendDirect(SendMessage sendMsg) {
        try {
            execute(sendMsg);
        } catch (TelegramApiException e) {
            logger.warn("Cant send direct message {}", e.toString());
            return false;
        }
        return true;
    }

    boolean sendDirect(SendPhoto sendPhoto) {
        if (sendPhoto == null) {
            return false;
        }
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.warn("Cant send direct message {}", e.toString());
            return false;
        }
        return true;
    }
}
