package ru.chernyshev.recognizer.service.recognize;

import ru.chernyshev.recognizer.RecognizeResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.io.File;
import java.util.function.Consumer;

public interface Recognizer {
    String recognize(File voiceFile, MessageType type, Consumer<RecognizeResult> entryConsumer);

    RecognizerType getType();

    boolean isApplicable(int duration);

    default int priority() {
        return Integer.MAX_VALUE;
    }
}
