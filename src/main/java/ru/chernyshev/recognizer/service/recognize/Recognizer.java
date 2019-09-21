package ru.chernyshev.recognizer.service.recognize;

import ru.chernyshev.recognizer.model.RecognizerType;

import java.io.File;

public interface Recognizer {
    String recognize(File voiceFile);

    RecognizerType getType();

    boolean isApplicable(int duration);
}
