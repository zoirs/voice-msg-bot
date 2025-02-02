package ru.chernyshev.recognizer.service;

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
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.entity.AdsInMsg;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.model.*;
import ru.chernyshev.recognizer.service.recognize.RecognizeFactory;
import ru.chernyshev.recognizer.service.recognize.Recognizer;
import ru.chernyshev.recognizer.utils.FromBuilder;
import ru.chernyshev.recognizer.utils.MessageKeys;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RecognizerBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);
    private static final Random random = new Random();

    private final ExecutorService service;
    private final RecognizeFactory recognizeFactory;
    private final String botToken;
    private final String botUsername;
    private final ChatService chatService;
    private final MessageService messageService;
    private final MessageValidator messageValidator;
    private final MessageSource messageSource;
    private final UserService userService;
    private final AdsForMsgService adsForMsgService;

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
                                AdsForMsgService adsForMsgService) {
        this.recognizeFactory = recognizeFactory;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatService = chatService;
        this.messageService = messageService;
        this.messageValidator = messageValidator;
        this.messageSource = messageSource;
        this.userService = userService;
        this.adsForMsgService = adsForMsgService;
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
                if (receivedMsg.getChat().isUserChat()) {
                    SendMessage sendMsg = new SendMessage();
                    sendMsg.setText("Перешлите сюда голосовое сообщение и оно преобразуется в текст.\n\n" +
                            "Добавьте бота в чат, и получайте текстовые расшифровки голосовых сообщений чата.\n\n" +
                            "Канал разработчика @chernyshev_ru");
                    sendMsg.setChatId(receivedMsg.getChatId());
                    sendDirect(sendMsg);
                }
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
        Integer duration = MessageService.getDuration(receivedMsg);
        logger.info("Message {} in chat {} has {} (duration {}, size {})", receivedMsg.getMessageId(), receivedMsg.getChatId(), type, duration, MessageService.getFileSize(receivedMsg));//байт , sec

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

        List<Recognizer> recognizers = recognizeFactory.create(duration, type);

        CompletableFuture
                .supplyAsync(() -> executeFile(fileId), service)
                .thenApply(file -> Recognize.apply(file, recognizers, type, result -> updateResult(messageEntity, result.getText(), result.getRecognizerType())))
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

    private boolean updateResult(MessageEntity messageEntity, String text, RecognizerType recognizerType) {
        if (MessageValidator.SKIP.equals(text)) {
            return false;
        }
        boolean isFinal = recognizerType != null;
        boolean recognizeSuccessfully = !StringUtils.isEmpty(text);
        if (!recognizeSuccessfully && !isFinal) {
            logger.info("Recognise unsuccessfully, but not final");
            return true;
        }

        String from = FromBuilder.create(messageEntity.getUser()).setItalic().get();
        ChatEntity chat = messageEntity.getChat();

        EditMessageText editMessage = new EditMessageText();
        editMessage.enableMarkdown(true);
        editMessage.setChatId(String.valueOf(chat.getTelegramId()));
        editMessage.setMessageId(messageEntity.getTelegramId());
        String adsMsg = "";
        if (recognizeSuccessfully && isFinal) {
            AdsInMsg adsInMsg = adsForMsgService.getAdsForMsg();
            if (adsInMsg != null && probability(adsInMsg)) {
                adsForMsgService.saveAdsSendInfo(adsInMsg.getId(), chat.getId(), messageEntity.getId());
                editMessage.disableWebPagePreview();
                adsMsg = "\n---\n" + adsInMsg.getText();
            }
        }
        editMessage.setText(from + (recognizeSuccessfully ? (text + adsMsg) : "Не распознано"));
        try {
            execute(editMessage);
            if (isFinal) {
                messageService.updateSuccess(messageEntity, recognizerType, editMessage.getMessageId());
            }
            return true;
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message to chat %s; %s", chat.getTelegramId(), e), e);
            messageService.update(messageEntity, MessageResult.CANT_UPDATE, editMessage.getMessageId());
            return false;
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
            if (e.toString().contains("have no rights to send a message") ||
                    e.toString().contains("not enough rights to send text messages to the chat")) {
                chatService.banned(chat);
            }
        }
        return null;
    }

    private static boolean probability(AdsInMsg adsInMsg) {
        if (adsInMsg.getPercent() >= 100) {
            return true;
        }
        if (adsInMsg.getPercent() <= 0) {
            return false;
        }
        return random.nextInt(100) <= adsInMsg.getPercent();
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
