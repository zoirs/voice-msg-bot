package ru.chernyshev.recognizer.entity;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getFinish() {
        return finish;
    }

    public void setFinish(LocalDateTime finish) {
        this.finish = finish;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
