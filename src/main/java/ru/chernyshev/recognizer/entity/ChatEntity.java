package ru.chernyshev.recognizer.entity;

import ru.chernyshev.recognizer.model.ChatStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "CHATS")
public class ChatEntity extends AbstractTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_chat")
    @SequenceGenerator(name = "seq_chat",
            sequenceName = "seq_chat", allocationSize = 1)
    private Long id;

    @Column(name = "telegram_id",nullable = false, unique = true)
    private Long telegramId;

    @Column
    private String userName;
    @Column
    private String firstName;
    @Column
    private String lastName;
    @Column
    private String groupType;
    @Column
    private String groupName;
    @Column
    private String locale = "ru";
    @Column
    private boolean removed = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatStatus state;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ChatStatus getState() {
        return state;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setState(ChatStatus state) {
        this.state = state;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }
}
