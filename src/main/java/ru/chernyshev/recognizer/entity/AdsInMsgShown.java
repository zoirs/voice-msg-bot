package ru.chernyshev.recognizer.entity;

import javax.persistence.*;

@Entity
@Table(name = "ads_in_msg_shown")
public class AdsInMsgShown {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_ads_in_msg_shown")
    @SequenceGenerator(name = "seq_ads_in_msg_shown",
            sequenceName = "seq_ads_in_msg_shown", allocationSize = 1)
    private Long id;

    @Column(name = "ads_in_msg_id", nullable = false)
    private Long adsInMsgId;
    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "message_id")
    private Long messageId;

    public AdsInMsgShown() {
    }

    public AdsInMsgShown(Long adsId, Long chatId, Long messageId) {
        this.adsInMsgId = adsId;
        this.chatId = chatId;
        this.messageId = messageId;
    }

    public Long getId() {
        return id;
    }

    public Long getAdsInMsgId() {
        return adsInMsgId;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getMessageId() {
        return messageId;
    }
}
