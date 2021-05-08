package ru.chernyshev.recognizer.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.chernyshev.recognizer.service.RecognizerBotService;
import ru.chernyshev.recognizer.service.recognize.Recognizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class Recognize implements Supplier<Entry<String, RecognizerType>> {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);

    private final File voiceFile;
    private final List<Recognizer> recognizers;

    public Recognize(File voiceFile, List<Recognizer> recognizers) {
        this.voiceFile = voiceFile;
        this.recognizers = recognizers;
    }

    @Override
    public Entry<String, RecognizerType> get() {
        String text = null;
        RecognizerType recognizerType = null;
        for (Recognizer recognizer : recognizers) {
            text = recognizer.recognize(voiceFile);
            recognizerType = recognizer.getType();
            if (!StringUtils.isEmpty(text)) {
                logger.info("Recognize {}: {}", recognizerType, org.apache.commons.lang3.StringUtils.substring(text, 0, 10));
                break;
            }
        }
        deleteFile(voiceFile);
        return new SimpleEntry<>(text, recognizerType);
    }

    private void deleteFile(File voiceFile) {
        try {
            Files.deleteIfExists(voiceFile.toPath());
        } catch (IOException e) {
            logger.warn("Cant delete file {}", voiceFile);
        }
    }
}
