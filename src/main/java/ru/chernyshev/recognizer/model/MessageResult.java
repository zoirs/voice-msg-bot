package ru.chernyshev.recognizer.model;

public enum MessageResult {
    PREPARE,
    SUCCESS,
    WITHOUT_VOICE,
    BANNED,
    VOICE_MSG_TOO_LONG,
    VOICE_MSG_TOO_HARD,
    CANT_EXECUTE_VOICE,
    CANT_RECOGNIZE,
    ERROR
}
