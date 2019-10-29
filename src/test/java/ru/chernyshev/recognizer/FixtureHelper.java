package ru.chernyshev.recognizer;

import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.model.ChatStatus;

public class FixtureHelper {

    public static ChatEntity createChat() {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setChatId(1L);
        chatEntity.setUserName("some");
        chatEntity.setFirstName("fn");
        chatEntity.setLastName("ln");
        chatEntity.setGroupType("s");
        chatEntity.setState(ChatStatus.ACTIVE);
        chatEntity.setGroupName("t");
        return chatEntity;
    }

    public static MessageEntity createMessage(ChatEntity chatEntity) {
        MessageEntity message = new MessageEntity();
        message.setChat(chatEntity);
        message.setDuration(2);
        return message;
    }

    public static UserEntity createUserEntity() {
        UserEntity from = new UserEntity();
        from.setId(1L);
        from.setFirstName("ss");
        from.setUserName("sw");
        from.setLastName("sd");
        return from;
    }
}
