package ru.chernyshev.recognizer.dto;

public class WitSpeech {
    private float confidence;
    private TokenWit[] tokens;

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public TokenWit[] getTokens() {
        return tokens;
    }

    public void setTokens(TokenWit[] tokens) {
        this.tokens = tokens;
    }
}
