package ru.chernyshev.recognizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.entity.Message;
import ru.chernyshev.recognizer.entity.User;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.UserStatus;
import ru.chernyshev.recognizer.repository.MessageRepository;
import ru.chernyshev.recognizer.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
public class RecognizerBotService extends TelegramLongPollingBot {

    private static Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);
    private static Integer ONE_MEGABYTE = 1024 * 1024;
    private static Integer ONE_MINUTE = 60;

    private final SpeechkitService speechkitService;
    private final String botToken;
    private final String botUsername;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public RecognizerBotService(SpeechkitService speechkitService,
                                @Value("${botToken}") String botToken,
                                @Value("${botUsername}") String botUsername, UserRepository userRepository, MessageRepository messageRepository) {
        this.speechkitService = speechkitService;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.trace("Received {}", update);

        Voice voice = update.getMessage().getVoice();
        Long chatId = update.getMessage().getChatId();
        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            user = new User();
            user.setChatId(chatId);
            user.setName(update.getMessage().getChat().getUserName());
            user.setState(UserStatus.ACTIVE);
            userRepository.save(user);
            logger.info(update.getMessage().getChat().toString());
        }
        Message message = new Message();
        message.setUser(user);
        if (voice != null) {
            message.setMessageType(MessageType.VOICE);
            logger.info("Message has voice {}", voice.toString());//байт , sec
            if (voice.getDuration() > ONE_MINUTE) {
                logger.info("Message too long: {}", voice.getDuration());
                sendMsg(chatId, "Вам недоступны сообщения длительностью более 1 минуты");
                message.setResult(MessageResult.ERROR);
                messageRepository.save(message);
                return;
            }
            if (voice.getFileSize() > ONE_MEGABYTE) {
                logger.warn("Message too big: {}", voice.getFileSize());
                sendMsg(chatId, "Вам недоступны объемные сообщения");
                message.setResult(MessageResult.ERROR);
                messageRepository.save(message);
                return;
            }

            File voiceFile = executeFile(voice);
            if (voiceFile == null) {
                logger.warn("No voice");
                message.setResult(MessageResult.ERROR);
                messageRepository.save(message);
                return;
            }
            String text;
            try {
                text = speechkitService.recognize(voiceFile);
            } catch (IOException e) {
                logger.error("Cant recognize message", e);
                sendMsg(chatId, "Не могу распознать сообщение");
                message.setResult(MessageResult.ERROR);
                messageRepository.save(message);
                return;
            } finally {
                deleteFile(voiceFile);
            }
            sendMsg(chatId, text);
            message.setResult(MessageResult.SUCCESS);
            messageRepository.save(message);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            message.setMessageType(MessageType.TEXT);
            message.setResult(MessageResult.ERROR);
            messageRepository.save(message);

            sendMsg(chatId, "Функция преобразования текста временно не доступна");
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
                .setText(text);
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
