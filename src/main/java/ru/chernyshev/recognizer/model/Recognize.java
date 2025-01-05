package ru.chernyshev.recognizer.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.chernyshev.recognizer.RecognizeResult;
import ru.chernyshev.recognizer.service.RecognizerBotService;
import ru.chernyshev.recognizer.service.recognize.Recognizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class Recognize {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);

    public static Entry<String, RecognizerType> apply(File voiceFile, List<Recognizer> recognizers, MessageType type, Consumer<RecognizeResult> progressCallback) {
        String text = null;
        RecognizerType recognizerType = null;
        for (Recognizer recognizer : recognizers) {
            text = recognizer.recognize(voiceFile, type, progressCallback);
            recognizerType = recognizer.getType();
            if (!StringUtils.isEmpty(text)) {
                logger.info("Recognize [{}] {}: {}...; (Length {})", recognizerType, type, org.apache.commons.lang3.StringUtils.substring(text, 0, 10), text.length());
                break;
            }
        }
        if (StringUtils.isEmpty(text)) {
            logger.warn("Recognize unsuccessful {}", voiceFile.length());
        }
        deleteFile(voiceFile);
        return new SimpleEntry<>(text, recognizerType);
    }

    private static void deleteFile(File voiceFile) {
        try {
            Files.deleteIfExists(voiceFile.toPath());
        } catch (IOException e) {
            logger.warn("Cant delete file {}", voiceFile);
        }
    }
}
