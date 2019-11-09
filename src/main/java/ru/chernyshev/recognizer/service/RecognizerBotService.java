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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.chernyshev.recognizer.entity.ChatEntity;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RecognizerBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(RecognizerBotService.class);

    private final ExecutorService service = Executors.newFixedThreadPool(2);

    private final RecognizeFactory recognizeFactory;
    private final String botToken;
    private final String botUsername;
    private final ChatService chatService;
    private final MessageService messageService;
    private final MessageValidator messageValidator;
    private final MessageSource messageSource;
    private final MessageRatingService ratingService;
    private final boolean turnRating;

    @Autowired
    public RecognizerBotService(RecognizeFactory recognizeFactory,
                                @Value("${botToken}") String botToken,
                                @Value("${botUsername}") String botUsername,
                                ChatService chatService,
                                MessageService messageService,
                                MessageValidator messageValidator,
                                MessageSource messageSource,
                                MessageRatingService ratingService,
                                @Value("${app.rating}") boolean turnRating) {
        this.recognizeFactory = recognizeFactory;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.chatService = chatService;
        this.messageService = messageService;
        this.messageValidator = messageValidator;
        this.messageSource = messageSource;
        this.ratingService = ratingService;
        this.turnRating = turnRating;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message receivedMsg = update.getMessage();
        if (turnRating && update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            LikeResult likeResult = ratingService.addLike(callbackQuery.getMessage().getMessageId(), callbackQuery.getFrom(), Integer.parseInt(callbackQuery.getData()));
            ChatEntity chat = chatService.getOrCreate(callbackQuery.getMessage().getChat());
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                    .setCallbackQueryId(callbackQuery.getId())
                    .setText(messageSource.getMessage(likeResult.name(), null, Locales.find(chat.getLocale())));
            try {
                execute(answerCallbackQuery);
            } catch (TelegramApiException e) {
                logger.error(String.format("Set rating error message %s, %s", callbackQuery.getMessage().getMessageId(), e.toString()), e);
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

        logger.info("Message has voice {}", voice.toString());//байт , sec

        ChatEntity chat = chatService.getOrCreate(receivedMsg.getChat());
        logger.info("create message {}", receivedMsg.getMessageId());
        MessageEntity entityMessage = messageService.create(chat, voice, receivedMsg.getMessageId());

        MessageResult validateResult = messageValidator.validate(chat, voice);
        if (validateResult != null) { // Нужно ли отправлять нотификации об ошибках?
            messageService.update(entityMessage, validateResult);
            return;
        }

        String text = messageSource.getMessage(MessageKeys.WAIT, null, Locales.find(chat.getLocale()));
        Message initMessage = sendMsg(receivedMsg.getChatId(), text);
        if (initMessage != null) {
            messageService.update(entityMessage, MessageResult.WAIT);
        } else {
            messageService.update(entityMessage, MessageResult.SEND_ERROR);
            logger.error("Cant send init message");
            return;
        }

        List<Recognizer> recognizers = recognizeFactory.create(voice.getDuration());
        String from = FromBuilder.create(receivedMsg).setItalic().get();

        CompletableFuture
                .supplyAsync(() -> executeFile(voice), service)
                .thenApply(file -> new Recognize(file, recognizers).get())
                .thenAccept(result -> updateResult(entityMessage, initMessage, from, result.getKey(), result.getValue()));

    }

    // разделить на два метода, отправку, и обновление ентити
    private void updateResult(MessageEntity entity, Message initMessage, String from, String text, RecognizerType recognizerType) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.enableMarkdown(true);
            editMessage.setChatId(initMessage.getChatId());
            editMessage.setMessageId(initMessage.getMessageId());
            boolean recognizeSuccessfully = !StringUtils.isEmpty(text);
            editMessage.setText(from + (recognizeSuccessfully ? text : "Не распознано"));
            if (turnRating && recognizeSuccessfully) {
                InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();// делать по настройке
                InlineKeyboardButton dislike = new InlineKeyboardButton("\uD83D\uDC4E").setCallbackData("-1");
                InlineKeyboardButton like = new InlineKeyboardButton("\uD83D\uDC4D").setCallbackData("1");
                editMessage.setText(from + text + "\n\n _" + messageSource.getMessage("give.rating", null, Locales.find(entity.getChat().getLocale())) + ":_");
                replyMarkup.getKeyboard().add(Lists.newArrayList(dislike, like));
                editMessage.setReplyMarkup(replyMarkup);
            }
            execute(editMessage);
            messageService.updateSuccess(entity, recognizerType, editMessage.getMessageId());
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message %s", e.toString()), e);
            messageService.update(entity, MessageResult.CANT_UPDATE);
        }
    }

    private Message sendMsg(Long chatId, String text) {
        if (StringUtils.isEmpty(text)) {
            logger.error("Cant send message empty msg");
            return null;
        }
        if (chatId == null) {
            logger.error("Unknown chat id");
            return null;
        }
        SendMessage message = new SendMessage()
                .setChatId(chatId)
                .enableMarkdown(true)
                .setText(Strings.capitalize(text));
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Cant send message to chat %s, error %s", chatId, e.toString()), e);
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
            logger.error("Cant load file", e);
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
