package ru.chernyshev.recognizer.service;

import io.jsonwebtoken.lang.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.model.ChatStatus;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.service.recognize.RecognizeFactory;
import ru.chernyshev.recognizer.service.recognize.Recognizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Component
public class RecognizerBotService extends TelegramLongPollingBot {

    private static Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);
    private static final int MAX_SIZE = 1024 * 1024;
    private static final int MAX_SECONDS = 59;
    private static final int MAX_MESSAGES_PER_DAY = 200;

    private final RecognizeFactory recognizeFactory;
    private final String botToken;
    private final String botUsername;
    private final ChatService chatService;
    private final MessageService messageService;

    @Autowired
    public RecognizerBotService(RecognizeFactory recognizeFactory,
                                @Value("${botToken}") String botToken,
                                @Value("${botUsername}") String botUsername,
                                ChatService chatService,
                                MessageService messageService) {
        this.recognizeFactory = recognizeFactory;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatService = chatService;
        this.messageService = messageService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message receivedMsg = update.getMessage();

        if (receivedMsg == null) {
            // Обычно, это редактирование сообщения
            return;
        }

        Voice voice = receivedMsg.getVoice();

        if (voice == null) {
            // Возможно, для статистики не плохо было бы собирать данные
            return;
        }

        logger.info("Message has voice {}", voice.toString());//байт , sec

        Long chatId = receivedMsg.getChatId();

        ChatEntity chat = chatService.getOrCreate(receivedMsg.getChat());
        MessageEntity message = messageService.create(chat);

        if (!"audio/ogg".equals(voice.getMimeType())) {
            logger.info("Incorrect audio format {}", voice.getMimeType());
            messageService.update(message, MessageResult.VOICE_MSG_INCORRECT_FORMAT);
            return;
        }

        if (chat.getState() != ChatStatus.ACTIVE) {
            logger.info("Chat {} is {}", chat.getId(), chat.getState());
            messageService.update(message, MessageResult.BANNED);
            return;
        }

        Long messageToday = chatService.getMessagesToday(chat);
        if (messageToday > MAX_MESSAGES_PER_DAY) {
            logger.info("Chat {} send {} today", chat.getId(), messageToday);
            sendMsg(chatId, "Превышено ограничение " + MAX_MESSAGES_PER_DAY + " сообщение в день");
            messageService.update(message, MessageResult.VOICE_MSG_TOO_MUCH_TODAY);
            return;
        }

        Integer duration = voice.getDuration();
        if (duration > MAX_SECONDS) {
            logger.info("Message too long: {}", duration);
            sendMsg(chatId, "Вам недоступны сообщения длительностью более " + MAX_SECONDS + " секунд");
            messageService.update(message, MessageResult.VOICE_MSG_TOO_LONG);
            return;
        }
        if (voice.getFileSize() > MAX_SIZE) {
            logger.warn("Message too big: {}", voice.getFileSize());
            sendMsg(chatId, "Вам недоступны объемные сообщения");
            messageService.update(message, MessageResult.VOICE_MSG_TOO_HARD);
            return;
        }

        File voiceFile = executeFile(voice);
        if (voiceFile == null || !voiceFile.exists()) {
            logger.warn("No voice {}", voice.getFileId());
            messageService.update(message, MessageResult.CANT_EXECUTE_VOICE);
            return;
        }

        String text = null;
        RecognizerType recognizerType = null;
        List<Recognizer> recognizers = recognizeFactory.create(duration);
        for (Recognizer recognizer : recognizers) {
            text = recognizer.recognize(voiceFile);
            recognizerType = recognizer.getType();
            if (!StringUtils.isEmpty(text)) {
                logger.info("Recognize {}: {}", recognizerType, text);
                break;
            }
        }

        deleteFile(voiceFile);

        if (StringUtils.isEmpty(text)) {
            sendMsg(chatId, "Не распознано");
            messageService.update(message, MessageResult.CANT_RECOGNIZE);
        } else {
            sendMsg(chatId, text);
            messageService.updateSuccess(message, recognizerType);
        }
    }

    private void deleteFile(File voiceFile) {
        try {
            Files.deleteIfExists(voiceFile.toPath());
        } catch (IOException e) {
            logger.warn("Cant delete file {}", voiceFile);
        }
    }

    private void sendMsg(Long chatId, String text) {
        if (StringUtils.isEmpty(text)) {
            logger.error("Cant send message empty msg");
            return;
        }
        if (chatId == null) {
            logger.error("Unknown chat id");
            return;
        }
        SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                .setChatId(chatId)
                .setText(Strings.capitalize(text));
        try {
            execute(message); // Call method to send the message
        } catch (TelegramApiException e) {
            logger.error("Cant send message", e);
        }
    }

    private File executeFile(Voice voice) {
        String uploadedFileId = voice.getFileId();
        final GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(uploadedFileId);
        try {
            final org.telegram.telegrambots.meta.api.objects.File voiceFile = execute(getFileMethod);
            return downloadFile(voiceFile.getFilePath());
        } catch (final TelegramApiException e) {
            logger.error("Cant load file", e);
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
