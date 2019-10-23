package ru.chernyshev.recognizer.service;

import java.util.Locale;

public enum Locales {
    RU(new Locale("RU")),
    EN(new Locale("EN"));

    private final Locale locale;

    Locales(Locale locale) {
        this.locale = locale;
    }

    public static Locale find(String name) {
        for (Locales value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value.locale;
            }
        }
        return RU.locale;
    }
}