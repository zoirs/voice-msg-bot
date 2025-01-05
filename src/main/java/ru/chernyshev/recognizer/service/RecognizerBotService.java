package ru.chernyshev.recognizer.service;

import com.google.common.collect.Lists;
import io.jsonwebtoken.lang.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.RecognizeResult;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.model.*;
import ru.chernyshev.recognizer.service.recognize.RecognizeFactory;
import ru.chernyshev.recognizer.service.recognize.Recognizer;
import ru.chernyshev.recognizer.utils.FromBuilder;
import ru.chernyshev.recognizer.utils.MessageKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class RecognizerBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);

    private final ExecutorService service;
    private final RecognizeFactory recognizeFactory;
    private final String botToken;
    private final String botUsername;
    private final ChatService chatService;
    private final MessageService messageService;
    private final MessageValidator messageValidator;
    private final MessageSource messageSource;
    private final UserService userService;
    private final AdsService adsService;

    @Autowired
    public RecognizerBotService(RecognizeFactory recognizeFactory,
                                @Value("${botToken}") String botToken,
                                @Value("${botUsername}") String botUsername,
                                @Value("${threads.count}") int threadCount,
                                ChatService chatService,
                                MessageService messageService,
                                MessageValidator messageValidator,
                                MessageSource messageSource,
                                UserService userService,
                                AdsService adsService) {
        this.recognizeFactory = recognizeFactory;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatService = chatService;
        this.messageService = messageService;
        this.messageValidator = messageValidator;
        this.messageSource = messageSource;
        this.userService = userService;
        this.adsService = adsService;
        this.service = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message receivedMsg = update.getMessage();
        if (receivedMsg == null) { // Обычно, это редактирование сообщения
            return;
        }

        if (receivedMsg.getEntities() != null
                && receivedMsg.getEntities().stream().anyMatch(q -> "bot_command".equalsIgnoreCase(q.getType()) && "/start".equalsIgnoreCase(q.getText()))) {
            logger.info("New user/chat add bot");
            User from = receivedMsg.getFrom();
            if (from != null) {
                logger.info("New user add bot {}", from);
                userService.getOrCreate(from);
            }
            Chat chat = receivedMsg.getChat();
            if (chat != null) {
                logger.info("New chat add bot {}", chat);
                chatService.getOrCreate(chat);
            }
            return;
        }

        if (receivedMsg.getLeftChatMember() != null && receivedMsg.getLeftChatMember().getUserName().equals(getBotUsername())) {
            logger.info("Was removed {}", receivedMsg.getChat());
            ChatEntity chat = chatService.getOrCreate(receivedMsg.getChat());
            chatService.remove(chat);
            return;
        }

        String fileId = MessageService.getFileId(receivedMsg);
        if (fileId == null) { // Возможно, для статистики не плохо было бы собирать данные
            return;
        }

        MessageType type = MessageService.getType(receivedMsg);
        logger.info("Message {} in chat {} has {} (duration {}, size {})", receivedMsg.getMessageId(), receivedMsg.getChatId(), type, MessageService.getDuration(receivedMsg), MessageService.getFileSize(receivedMsg));//байт , sec

        MessageEntity messageEntity = messageService.create(receivedMsg);

        MessageResult validateResult = messageValidator.validate(receivedMsg);
        if (validateResult != null) { // Нужно ли отправлять нотификации об ошибках?
            messageService.update(messageEntity, validateResult, receivedMsg.getMessageId());
            return;
        }

        String text = messageSource.getMessage(MessageKeys.WAIT, null, messageService.getLocale(messageEntity));
        Message initMessage = sendMsg(text, receivedMsg);
        if (initMessage != null) {
            messageService.update(messageEntity, MessageResult.WAIT, initMessage.getMessageId());
        } else {
            messageService.update(messageEntity, MessageResult.SEND_ERROR, null);
            logger.error("Cant send init message");
            return;
        }

        List<Recognizer> recognizers = recognizeFactory.create(MessageService.getDuration(receivedMsg), type);

        Consumer<RecognizeResult> entryConsumer = result -> updateResult(messageEntity, result.getText(), result.getRecognizerType());
        CompletableFuture
                .supplyAsync(() -> executeFile(fileId), service)
                .thenApply(file -> Recognize.apply(file, recognizers, type, entryConsumer))
                .thenAccept(result -> updateResult(messageEntity, result.getKey(), result.getValue()))
        ;
    }

    boolean sendDirect(SendPhoto sendPhoto) {
        if (sendPhoto == null) {
            return false;
        }
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.warn("Cant send direct message {}", e.toString());
            return false;
        }
        return true;
    }

    private void updateResult(MessageEntity messageEntity, String text, RecognizerType recognizerType) {

        String from = FromBuilder.create(messageEntity.getUser()).setItalic().get();
        ChatEntity chat = messageEntity.getChat();

        EditMessageText editMessage = new EditMessageText();
        editMessage.enableMarkdown(true);
        editMessage.setChatId(String.valueOf(chat.getTelegramId()));
        editMessage.setMessageId(messageEntity.getTelegramId());
        boolean recognizeSuccessfully = !StringUtils.isEmpty(text);
        editMessage.setText(from + (recognizeSuccessfully ? text : "Не распознано"));
        if (recognizeSuccessfully) {
            AdsButton current = adsService.getCurrent();
            if (isNeedShowAds(chat, current)) {
                InlineKeyboardButton adsButton = new InlineKeyboardButton(current.getText());
                adsButton.setUrl(current.getUrl());
                List<List<InlineKeyboardButton>> keyBoard = new ArrayList<>();
                keyBoard.add(Lists.newArrayList(adsButton));
                InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup(keyBoard);
                editMessage.setReplyMarkup(replyMarkup);
                adsService.saveAdsSendInfo(current.getId(), chat.getId(), messageEntity.getId(), true);
            }
        }
        try {
            execute(editMessage);
            if (recognizerType != null) {
                messageService.updateSuccess(messageEntity, recognizerType, editMessage.getMessageId());
            }
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message %s", e.toString()), e);
            messageService.update(messageEntity, MessageResult.CANT_UPDATE, editMessage.getMessageId());
        }
    }

    private Message sendMsg(String text, Message receivedMsg) {
        if (StringUtils.isEmpty(text)) {
            logger.error("Cant send message empty msg");
            return null;
        }
        Chat chat = receivedMsg.getChat();
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chat.getId()));
        message.enableMarkdown(true);
        if (receivedMsg.isTopicMessage()) {
            // Топик супер группы
            message.setMessageThreadId(receivedMsg.getMessageThreadId());
//            logger.info("Chat with topics {}", receivedMsg);
        } else if (receivedMsg.getMessageThreadId() != null) {
            // Канал с прилинкованной группой с комментариями
            message.setReplyToMessageId(receivedMsg.getMessageId());
        }
        message.setText(Strings.capitalize(text));
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message to chat %s %s, error %s", chat.getId(), chat.getTitle(), e), e);
            if (e.toString().contains("have no rights to send a message")) {
                chatService.banned(chat);
            }
        }
        return null;
    }

    private boolean isNeedShowAds(ChatEntity chat, AdsButton current) {
        return current != null && current.getType() == AdsType.RECOGNIZER_MESSAGE
                && (current.getTestChatId() == null || chat.getId().equals(current.getTestChatId()));
    }

    private File executeFile(String uploadedFileId) {
        final GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(uploadedFileId);
        try {
            final org.telegram.telegrambots.meta.api.objects.File voiceFile = execute(getFileMethod);
            return downloadFile(voiceFile.getFilePath());
        } catch (final TelegramApiException e) {
            logger.error(String.format("Cant load file %s, %s", uploadedFileId, e.toString()), e);
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    public boolean sendDirect(SendMessage sendMsg) {
        try {
            execute(sendMsg);
        } catch (TelegramApiException e) {
            logger.warn("Cant send direct message {}", e.toString());
            return false;
        }
        return true;
    }
}
