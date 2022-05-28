package ru.chernyshev.recognizer.entity;

import javax.annotation.Nullable;
import javax.persistence.*;

@Entity
@Table(name = "ADS_SENDED")
public class AdsSended {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_ads_sended")
    @SequenceGenerator(name = "seq_ads_sended",
            sequenceName = "seq_ads_sended", allocationSize = 1)
    private Long id;

    private Long adsId;
    private Long chatId;
    @Nullable
    private Long messageId;

    private boolean isSuccess;

    public AdsSended() {
    }

    public AdsSended(Long adsId, Long chatId, boolean isSuccess) {
        this.adsId = adsId;
        this.chatId = chatId;
        this.isSuccess = isSuccess;
    }

    public AdsSended(Long adsId, Long chatId, Long messageId, boolean isSuccess) {
        this(adsId, chatId, isSuccess);
        this.messageId = messageId;
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

    @Nullable
    public Long getMessageId() {
        return messageId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
