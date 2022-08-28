package ru.chernyshev.recognizer.dto;

public class WitAiChunkResponse {
      private boolean is_final;
      private WitSpeech speech;
      private String text;

    public boolean isIs_final() {
        return is_final;
    }

    public void setIs_final(boolean is_final) {
        this.is_final = is_final;
    }

    public WitSpeech getSpeech() {
        return speech;
    }

    public void setSpeech(WitSpeech speech) {
        this.speech = speech;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
