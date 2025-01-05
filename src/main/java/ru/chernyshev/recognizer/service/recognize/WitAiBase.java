package ru.chernyshev.recognizer.service.recognize;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;
import ru.chernyshev.recognizer.RecognizeResult;
import ru.chernyshev.recognizer.model.MessageType;
import ru.chernyshev.recognizer.service.MessageValidator;
import ru.chernyshev.recognizer.utils.FfmpegCommandBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class WitAiBase implements Recognizer {

    private static final Logger logger = LoggerFactory.getLogger(WitAiBase.class);

    private final String ffmpeg;
    private final RestTemplate restTemplate;
    private final List<WitSettings> settings;
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper;

    private final RateLimiter rateLimiter;

    private int countOfUse;

    public WitAiBase(String ffmpeg,
                     List<String> configs,
                     Environment env,
                     RestTemplate restTemplate,
                     ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.rateLimiter = RateLimiter.create(1);
        this.ffmpeg = ffmpeg;
        this.settings = configs.stream()
                .map(q -> new WitSettings(q, env.getProperty(q + ".witat.url"), env.getProperty(q + ".witat.auth")))
                .collect(Collectors.toList());
        this.restTemplate = restTemplate;
    }


    @Override
    public String recognize(File fileVoice, MessageType type, Consumer<RecognizeResult> entryConsumer) {
        FfmpegCommandBuilder builder = new FfmpegCommandBuilder(ffmpeg, fileVoice.getAbsolutePath());
        int volume = builder.getVolume();
        if (type == MessageType.VOICE) {
            builder.withAudioSettings(volume);
        }
        if (type == MessageType.VIDEO) {
            builder.withVideoNoteSettings(volume);
        }

        File file = builder.execute();
        if (file == null || !file.exists()) {
            logger.error("Ffmpeg cant process file");
            return null;
        }

        byte[] bytes;
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            logger.error("Cant read file " + file, e);
            return null;
        } finally {
            deleteFile(file);
        }

        for (WitSettings setting : settings) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(setting.getAuth());
            headers.set("Content-Type", "audio/ogg");
            headers.set("Transfer-encoding", "chunked");
            if (!rateLimiter.tryAcquire()) {
                logger.warn("Rate too much {}", rateLimiter.getRate());
                MessageValidator.sleep();
            }
            countOfUse++;
            try {
                RequestCallback requestCallback = request -> {
                    request.getHeaders().addAll(headers);
                    request.getBody().write(bytes);
                };
                return restTemplate.execute(URI.create(setting.getUrl()), HttpMethod.POST, requestCallback, clientHttpResponse -> {
                    try (InputStream inputStream = clientHttpResponse.getBody()) {
                        String lastText = "";
                        JsonParser jp = jsonFactory.createParser(inputStream);
                        jp.setCodec(objectMapper);
                        jp.nextToken();
                        while (jp.currentToken() != null) {
                            if (jp.currentToken() == JsonToken.START_OBJECT) {
                                JsonNode node = jp.readValueAsTree();
                                String currentText = node.has("text") ? node.get("text").asText() : null;
                                if (!StringUtils.isEmpty(currentText)) {
                                    if (node.has("is_final") && node.get("is_final").asBoolean()) {
                                        return currentText;
                                    } else if (!currentText.equals(lastText) &&  node.has("speech")) {
                                        lastText = currentText;
                                        entryConsumer.accept(new RecognizeResult(currentText + " ⏳", null));
                                    }
                                }

                                if (node.has("code")) {
                                    logger.error("Recognize {} symbols and got error: {}", lastText.length(), node);
                                    return StringUtils.isEmpty(lastText) ? lastText : (lastText + "... \n/_расшифрованно не полностью_/");
                                }
                            }

                            jp.nextToken();
                        }
                        return lastText;
                    }
                });
            } catch (Exception e) {
                logger.error("Cant send request to wit ai [" + setting.getName() + "] ", e);
            }
        }
        return null;
    }

    private void deleteFile(File voiceFile) {
        try {
            Files.deleteIfExists(voiceFile.toPath());
        } catch (IOException e) {
            logger.warn("Cant delete file {}", voiceFile);
        }
    }

    @Override
    public boolean isApplicable(int duration) {
        return duration < MessageValidator.MAX_SECONDS;
    }

    public int priority() {
        return countOfUse;
    }
}