package ru.chernyshev.recognizer.service.recognize;

import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.io.File;

public interface Recognizer {
    String recognize(File voiceFile, MessageType type);

    RecognizerType getType();

    boolean isApplicable(int duration);

    default int priority() {
        return Integer.MAX_VALUE;
    }
}
