package ru.chernyshev.recognizer.model;

import java.time.LocalDateTime;

public class AdsButton {
    private final LocalDateTime start;
    private final LocalDateTime finish;
    private final String url;
    private final String text;

    public AdsButton(LocalDateTime start, LocalDateTime finish, String url, String text) {
        this.start = start;
        this.finish = finish;
        this.url = url;
        this.text = text;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getFinish() {
        return finish;
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }

    public boolean isExpiry() {
        return finish.isBefore(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "AdsButton{" +
                "start=" + start +
                ", finish=" + finish +
                ", url='" + url + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
