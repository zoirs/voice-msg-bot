package ru.chernyshev.recognizer.entity;

import ru.chernyshev.recognizer.model.AdsType;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADS")
public class AdsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_ads")
    @SequenceGenerator(name = "seq_ads",
            sequenceName = "seq_ads", allocationSize = 1)
    private Long id;

    @Column(name = "start", nullable = false)
    private LocalDateTime start;

    @Column(name = "finish", nullable = false)
    private LocalDateTime finish;

    @Column(name = "url")
    private String url;

    @Column(name = "text")
    private String text;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "max_count")
    private Long maxCount;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AdsType type;

    @Column(name = "test_chat_id")
    private Long testChatId;


    public AdsEntity() {
    }

    public AdsEntity(LocalDateTime start, LocalDateTime finish, String url, String text, String filePath, Long maxCount, AdsType type) {
        this.start = start;
        this.finish = finish;
        this.url = url;
        this.text = text;
        this.filePath = filePath;
        this.maxCount = maxCount;
        this.type = type;
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

    public Long getMaxCount() {
        return maxCount;
    }

    public AdsType getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getTestChatId() {
        return testChatId;
    }
}
