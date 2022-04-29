package ru.chernyshev.recognizer.entity;

import javax.persistence.*;

@Entity
@Table(name = "ADS_DIRECT")
public class AdsDirectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_ads_direct")
    @SequenceGenerator(name = "seq_ads_direct",
            sequenceName = "seq_ads_direct", allocationSize = 1)
    private Long id;

    private Long adsId;
    private Long chatId;

    private boolean isSuccess;

    public AdsDirectEntity() {
    }

    public AdsDirectEntity(Long adsId, Long chatId, boolean isSuccess) {
        this.adsId = adsId;
        this.chatId = chatId;
        this.isSuccess = isSuccess;
    }

    public Long getId() {
        return id;
    }

    public Long getAdsId() {
        return adsId;
    }

    public Long getChatId() {
        return chatId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
