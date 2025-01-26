package ru.chernyshev.recognizer.service.recognize;

public class WitSettings {

    private final String name;
    private final String url;
    private final String auth;
    private final int maxSeconds;

    public WitSettings(String name, String url, String auth, int maxSeconds) {
        this.name = name;
        this.url = url;
        this.auth = auth;
        this.maxSeconds = maxSeconds;
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

    public int getMaxSeconds() {
        return maxSeconds;
    }

    @Override
    public String toString() {
        return "WitSettings{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", auth='" + auth + '\'' +
                ", maxSeconds=" + maxSeconds +
                '}';
    }
}
