package ru.chernyshev.recognizer.model;

public enum MessageResult {
    PREPARE,
    SUCCESS,
    WITHOUT_VOICE,
    BANNED,
    VOICE_MSG_TOO_LONG,
    VOICE_MSG_TOO_HARD,
    VOICE_MSG_TOO_MUCH_TODAY,
    CANT_EXECUTE_VOICE,
    CANT_RECOGNIZE,
    SYSTEM_MSG,
    ERROR
}
