package ru.chernyshev.recognizer.service.recognize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.RecognizeResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.io.File;
import java.time.LocalDateTime;
import java.util.function.Function;

import static ru.chernyshev.recognizer.model.RecognizerType.TEST;

@Service
public class TestRecognizer implements Recognizer {

    private static final Logger logger = LoggerFactory.getLogger(TestRecognizer.class);

    @Override
    public String recognize(File voiceFile, MessageType type, Function<RecognizeResult, Boolean> entryConsumer) {
        logger.info("Test recognize");
        return "Тест распознования " + LocalDateTime.now();
    }

    @Override
    public RecognizerType getType() {
        return TEST;
    }

    @Override
    public boolean isApplicable(int duration) {
        return true;
    }
}
