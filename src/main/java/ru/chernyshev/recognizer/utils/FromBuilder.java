package ru.chernyshev.recognizer.utils;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.springframework.util.StringUtils.isEmpty;

public class FromBuilder {
    private final User user;
    private Prefix textType = Prefix.NONE;

    private FromBuilder(User user) {
        this.user = user;
    }

    public static FromBuilder create(Message receivedMsg) {
        if (receivedMsg.getForwardFrom() != null) {
            return new FromBuilder(receivedMsg.getForwardFrom());
        } else {
            return new FromBuilder(receivedMsg.getFrom());
        }
    }

    public FromBuilder setItalic() {
        this.textType = Prefix.ITALIC;
        return this;
    }

    public String get() {
        if (isEmpty(user.getFirstName()) && isEmpty(user.getLastName())) {
            return "";
        }
        StringBuilder result = new StringBuilder("от ");
        if (!isEmpty(user.getFirstName())) {
            result
                    .append(textType.prefix)
                    .append(user.getFirstName())
                    .append(textType.prefix)
                    .append(" ");
        }
        if (!isEmpty(user.getLastName())) {
            result
                    .append(textType.prefix)
                    .append(user.getLastName())
                    .append(textType.prefix);
        }
        result.append(":" + "\n");
        return result.toString();
    }


    private enum Prefix {
        NONE(""),
        BOLD("*"),
        ITALIC("_");

        private final String prefix;

        Prefix(String prefix) {
            this.prefix = prefix;
        }
    }

}
