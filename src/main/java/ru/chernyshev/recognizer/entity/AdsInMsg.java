package ru.chernyshev.recognizer.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ads_in_msg")
public class AdsInMsg {

    @Id
    private Long id;
    
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "text")
    private String text;

    @Column(name = "percent")
    private Long percent;

    public AdsInMsg() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDay() {
        return day;
    }

    public String getText() {
        return text;
    }

    public Long getPercent() {
        return percent;
    }
}
