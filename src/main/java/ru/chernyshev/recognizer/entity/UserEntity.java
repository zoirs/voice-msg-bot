package ru.chernyshev.recognizer.entity;

import ru.chernyshev.recognizer.model.ChatStatus;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "USERS")
public class UserEntity {//extends AbstractTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "seq_users")
    @SequenceGenerator(name = "seq_users",
            sequenceName = "seq_users", allocationSize = 1)
    private Long id;

    @Column
    private Long telegramId;
    @Column
    private String userName;
    @Column
    private String firstName;
    @Column
    private String lastName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

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
}
