package ru.chernyshev.recognizer.model;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class AimToken {
    private LocalDateTime expiresAt;
    private String iamToken;

    public AimToken() {
    }

    public boolean isActual() {
        return !StringUtils.isEmpty(iamToken) && expiresAt != null && !expiresAt.isBefore(LocalDateTime.now());
    }

    public String getIamToken() {
        return iamToken;
    }

    public void setIamToken(String iamToken) {
        this.iamToken = iamToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        ZonedDateTime zdt = ZonedDateTime.parse(expiresAt);
        this.expiresAt = zdt.toLocalDateTime();
    }
}
