package ru.chernyshev.recognizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;

@Component
public class RecognizerBot extends TelegramLongPollingBot {

    private static Logger logger = LoggerFactory.getLogger(RecognizerBot.class);

    private final SpeechkitService speechkitService;

    @Autowired
    public RecognizerBot(SpeechkitService speechkitService) {
        this.speechkitService = speechkitService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.trace("Received {}", update);

        Voice voice = update.getMessage().getVoice();
        Long chatId = update.getMessage().getChatId();
        if (voice != null) {
            logger.trace("Message has voice");
            File voiceFile = executeFile(voice);
            if (voiceFile == null) {
                logger.trace("Message has voice");
                return;
            }
            String text;
            try {
                text = speechkitService.recognize(voiceFile);
            } catch (IOException e) {
                logger.error("Cant recognize message", e);
                sendMsg(chatId, "Не могу распознать сообщение");
                return;
            }
            sendMsg(chatId, text);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            sendMsg(chatId, "Функция преобразования текста временно не доступна");
        }
    }

    private void sendMsg(Long chatId, String text) {
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
        return "RecTestBot";
    }

    @Override
    public String getBotToken() {
        return "709753729:AAEmCZ0_DoZuMHRMYykXvPl7DVZtYt-nYVA";
    }
}
