package ru.chernyshev.recognizer;

import ru.chernyshev.recognizer.model.RecognizerType;

public class RecognizeResult {
    private final String text;
    private final RecognizerType recognizerType;

    public RecognizeResult(String text, RecognizerType recognizerType) {
        this.text = text;
        this.recognizerType = recognizerType;
    }

    public String getText() {
        return text;
    }

    public RecognizerType getRecognizerType() {
        return recognizerType;
    }
}
