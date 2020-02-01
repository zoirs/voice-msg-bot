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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.entity.ChatEntity;
import ru.chernyshev.recognizer.entity.LikeEntity;
import ru.chernyshev.recognizer.entity.LikeResult;
import ru.chernyshev.recognizer.entity.MessageEntity;
import ru.chernyshev.recognizer.model.MessageResult;
import ru.chernyshev.recognizer.model.Recognize;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.service.recognize.RecognizeFactory;
import ru.chernyshev.recognizer.service.recognize.Recognizer;
import ru.chernyshev.recognizer.utils.FromBuilder;
import ru.chernyshev.recognizer.utils.MessageKeys;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RecognizerBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);

    private final ExecutorService service = Executors.newFixedThreadPool(4);

    private final RecognizeFactory recognizeFactory;
    private final String botToken;
    private final String botUsername;
    private final ChatService chatService;
    private final MessageService messageService;
    private final MessageValidator messageValidator;
    private final MessageSource messageSource;
    private final MessageRatingService ratingService;

    private final boolean turnRating;
    private final String donatUrl;

    @Autowired
    public RecognizerBotService(RecognizeFactory recognizeFactory,
                                @Value("${botToken}") String botToken,
                                @Value("${botUsername}") String botUsername,
                                ChatService chatService,
                                MessageService messageService,
                                MessageValidator messageValidator,
                                MessageSource messageSource,
                                MessageRatingService ratingService,
                                @Value("${app.rating}") boolean turnRating,
                                @Value("${give.donat.url}") String donatUrl) {
        this.recognizeFactory = recognizeFactory;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatService = chatService;
        this.messageService = messageService;
        this.messageValidator = messageValidator;
        this.messageSource = messageSource;
        this.ratingService = ratingService;
        this.turnRating = turnRating;
        this.donatUrl = donatUrl;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message receivedMsg = update.getMessage();
        if (turnRating && update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            LikeEntity likeEntity = ratingService.addLike(callbackQuery.getMessage(), callbackQuery.getFrom(), Integer.parseInt(callbackQuery.getData()));
            String message = null;
            if (likeEntity != null) {
                message = messageSource.getMessage(LikeResult.LIKE_ADDED.name(), null, messageService.getLocale(likeEntity.getMessage()));
            }
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                    .setCallbackQueryId(callbackQuery.getId())
                    .setText(message);
            try {
                execute(answerCallbackQuery);
            } catch (TelegramApiException e) {
                logger.error(String.format("Set rating error message %s, %s", callbackQuery.getMessage().getMessageId(), e.toString()), e);
            }
            if (likeEntity != null) {
                Map<String, Integer> rating = ratingService.getRating(likeEntity.getMessage());
                updateRating(likeEntity.getMessage(), rating.get("like"), rating.get("dis"));
            }
            return;
        }
        if (receivedMsg == null) { // Обычно, это редактирование сообщения
            return;
        }

        if (receivedMsg.getLeftChatMember() != null && receivedMsg.getLeftChatMember().getUserName().equals(getBotUsername())) {
            logger.info("Was removed {}", receivedMsg.getChat());
            ChatEntity chat = chatService.getOrCreate(receivedMsg.getChat());
            chatService.remove(chat);
            return;
        }

        Voice voice = receivedMsg.getVoice();
        if (voice == null) { // Возможно, для статистики не плохо было бы собирать данные
            return;
        }

        logger.info("Message {} in chat {} has voice {}", receivedMsg.getMessageId(), receivedMsg.getChatId(), voice.toString());//байт , sec

        MessageEntity messageEntity = messageService.create(receivedMsg);

        MessageResult validateResult = messageValidator.validate(receivedMsg);
        if (validateResult != null) { // Нужно ли отправлять нотификации об ошибках?
            messageService.update(messageEntity, validateResult, receivedMsg.getMessageId());
            return;
        }

        String text = messageSource.getMessage(MessageKeys.WAIT, null, messageService.getLocale(messageEntity));
        Message initMessage = sendMsg(receivedMsg.getChat(), text);
        if (initMessage != null) {
            messageService.update(messageEntity, MessageResult.WAIT, initMessage.getMessageId());
        } else {
            messageService.update(messageEntity, MessageResult.SEND_ERROR, null);
            logger.error("Cant send init message");
            return;
        }

        List<Recognizer> recognizers = recognizeFactory.create(voice.getDuration());

        CompletableFuture
                .supplyAsync(() -> executeFile(voice), service)
                .thenApply(file -> new Recognize(file, recognizers).get())
                .thenAccept(result -> updateResult(messageEntity, result.getKey(), result.getValue()));
    }

    private void updateResult(MessageEntity messageEntity, String text, RecognizerType recognizerType) {

        String from = FromBuilder.create(messageEntity.getUser()).setItalic().get();

        EditMessageText editMessage = new EditMessageText();
        editMessage.enableMarkdown(true);
        editMessage.setChatId(messageEntity.getChat().getTelegramId());
        editMessage.setMessageId(messageEntity.getTelegramId());
        boolean recognizeSuccessfully = !StringUtils.isEmpty(text);
        editMessage.setText(from + (recognizeSuccessfully ? text : "Не распознано"));
        if (recognizeSuccessfully) {
            if (turnRating) {
                InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();// делать по настройке
                InlineKeyboardButton dislike = new InlineKeyboardButton("\uD83D\uDC4E").setCallbackData("-1");
                InlineKeyboardButton like = new InlineKeyboardButton("\uD83D\uDC4D").setCallbackData("1");
                editMessage.setText(from + text + "\n\n _" + messageSource.getMessage("give.rating", null, messageService.getLocale(messageEntity)) + ":_");
                replyMarkup.getKeyboard().add(Lists.newArrayList(dislike, like));
                editMessage.setReplyMarkup(replyMarkup);
            } else if (!StringUtils.isEmpty(donatUrl)) {
                if (LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY) {
                    InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
                    String giveDonateMsg = messageSource.getMessage("give.donate", null, messageService.getLocale(messageEntity));
                    InlineKeyboardButton donate = new InlineKeyboardButton(giveDonateMsg).setUrl(donatUrl);
                    replyMarkup.getKeyboard().add(Lists.newArrayList(donate));
                    editMessage.setReplyMarkup(replyMarkup);
                }
            }
        }
        try {
            execute(editMessage);
            messageService.updateSuccess(messageEntity, recognizerType, editMessage.getMessageId());
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message %s", e.toString()), e);
            messageService.update(messageEntity, MessageResult.CANT_UPDATE, editMessage.getMessageId());
        }
    }

    private void updateRating(MessageEntity messageEntity, int countLike, int countDis) {
        if (!turnRating) {
            return;
        }

        ChatEntity chatEntity = chatService.require(messageEntity.getChat().getId());
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(chatEntity.getTelegramId());
        editMessageReplyMarkup.setMessageId(messageEntity.getTelegramId());

        InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton dislike = new InlineKeyboardButton("\uD83D\uDC4E" + (countDis > 0 ? countDis : "")).setCallbackData("-1");
        InlineKeyboardButton like = new InlineKeyboardButton("\uD83D\uDC4D" + (countLike > 0 ? countLike : "")).setCallbackData("1");
        replyMarkup.getKeyboard().add(Lists.newArrayList(dislike, like));
        editMessageReplyMarkup.setReplyMarkup(replyMarkup);
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant update rating message %s", e.toString()), e);
        }
    }

    private Message sendMsg(Chat chat, String text) {
        if (StringUtils.isEmpty(text)) {
            logger.error("Cant send message empty msg");
            return null;
        }
        SendMessage message = new SendMessage()
                .setChatId(chat.getId())
                .enableMarkdown(true)
                .setText(Strings.capitalize(text));
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message to chat %s %s, error %s", chat.getId(), chat.getTitle(), e.toString()), e);
        }
        return null;
    }

    private File executeFile(Voice voice) {
        String uploadedFileId = voice.getFileId();
        final GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(uploadedFileId);
        try {
            final org.telegram.telegrambots.meta.api.objects.File voiceFile = execute(getFileMethod);
            return downloadFile(voiceFile.getFilePath());
        } catch (final TelegramApiException e) {
            logger.error(String.format("Cant load file %s, %s", voice, e.toString()), e);
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
}
