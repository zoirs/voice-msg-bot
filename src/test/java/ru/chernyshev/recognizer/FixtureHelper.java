package ru.chernyshev.recognizer;

import com.google.common.base.MoreObjects;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.entity.UserEntity;
import ru.chernyshev.recognizer.model.ChatStatus;

import javax.annotation.Nullable;

public class FixtureHelper {

    public static ChatEntity createChat(long chatId, @Nullable String groupType) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setTelegramId(chatId);
        chatEntity.setUserName("user_" + chatId);
        chatEntity.setFirstName("fn");
        chatEntity.setLastName("ln");
        chatEntity.setGroupType(MoreObjects.firstNonNull(groupType, getGroupType(chatId)));
        chatEntity.setState(ChatStatus.ACTIVE);
        chatEntity.setGroupName(getGroupType(chatId) + "_name");
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

    private static String getGroupType(long chatId) {
        int l = (int) (chatId % 3);
        switch (l) {
            case 0:
                return "supergroup";
            case 1:
                return "private";
            case 2:
                return "group";
        }
        return "group";
    }
}
