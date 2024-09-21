package ru.chernyshev.recognizer.model;

import ru.chernyshev.recognizer.entity.AdsEntity;

import java.time.LocalDateTime;

public class AdsButton {
    private final LocalDateTime start;
    private final LocalDateTime finish;
    private final String url;
    private final String text;
    private final Long id;
    private final AdsType type;
    private final Long maxCount;
    private final Long testChatId;
    private final String filePath;
    private long currentCount;

    public AdsButton(AdsEntity adsEntity) {
        this.start = adsEntity.getStart();
        this.finish = adsEntity.getFinish();
        this.url = adsEntity.getUrl();
        this.text = adsEntity.getText();
        this.id = adsEntity.getId();
        this.type = adsEntity.getType();
        this.maxCount = adsEntity.getMaxCount();
        this.filePath = adsEntity.getFilePath();
        this.testChatId = adsEntity.getTestChatId();
    }

    public Long getId() {
        return id;
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
        return finish.isBefore(LocalDateTime.now()) || maxCount > 0 && currentCount >= maxCount;
    }

    public AdsType getType() {
        return type;
    }

    public Long getMaxCount() {
        return maxCount;
    }

    public void inc() {
        currentCount++;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public Long getTestChatId() {
        return testChatId;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "AdsButton{" +
                "start=" + start +
                ", finish=" + finish +
                ", id=" + id +
                ", type=" + type +
                ", maxCount=" + maxCount +
                ", testChatId=" + testChatId +
                ", currentCount=" + currentCount +
                '}';
    }
}
