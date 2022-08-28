package ru.chernyshev.recognizer.service.recognize;

public class WitSettings {

    private final String name;
    private final String url;
    private final String auth;

    public WitSettings(String name, String url, String auth) {
        this.name = name;
        this.url = url;
        this.auth = auth;
    }

    public String getUrl() {
        return url;
    }

    public String getAuth() {
        return auth;
    }

    public String getName() {
        return name;
    }
}
