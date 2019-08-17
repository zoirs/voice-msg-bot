package ru.chernyshev.recognizer.entity;

import ru.chernyshev.recognizer.model.UserStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "USERS")
public class User extends AbstractTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_user")
    @SequenceGenerator(name = "seq_user",
            sequenceName = "seq_user", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long chatId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus state;

    @OneToMany(mappedBy = "user")
    private List<Message> userMessages;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public List<Message> getUserMessages() {
        return userMessages;
    }

    public void setUserMessages(List<Message> userMessages) {
        this.userMessages = userMessages;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public UserStatus getState() {
        return state;
    }

    public void setState(UserStatus state) {
        this.state = state;
    }
}
