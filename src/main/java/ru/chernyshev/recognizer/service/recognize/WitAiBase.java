package ru.chernyshev.recognizer.service.recognize;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class WitAiBase implements Recognizer {

    private static final Logger logger = LoggerFactory.getLogger(WitAiBase.class);
    private static final int MIN_UPDATE_INTERVAL = 1000;

    private final String ffmpeg;
    private final RestTemplate restTemplate;
    private final List<WitSettings> settings;
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper;

    private final RateLimiter rateLimiter;

    private int countOfUse;
    public boolean isForLongOnly;

    public WitAiBase(String ffmpeg,
                     List<String> configs,
                     Environment env,
                     RestTemplate restTemplate,
                     ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.rateLimiter = RateLimiter.create(1);
        this.ffmpeg = ffmpeg;
        this.settings = configs.stream()
                .map(q -> {
                    String maxSecProp = env.getProperty(q + ".witat.maxSeconds");
                    int maxSeconds = StringUtils.isEmpty(maxSecProp) ? 0 : Integer.parseInt(maxSecProp);
                    return new WitSettings(q, env.getProperty(q + ".witat.url"), env.getProperty(q + ".witat.auth"), maxSeconds);
                })
                .collect(Collectors.toList());
        this.restTemplate = restTemplate;
        Preconditions.checkArgument(this.settings.size() == 1, "More than one settings " + settings.stream().map(WitSettings::toString).collect(Collectors.joining()));
        isForLongOnly = settings.get(0).getMaxSeconds() > 41;
    }


    @Override
    public String recognize(File fileVoice, MessageType type, Function<RecognizeResult, Boolean> progressCallback) {
//        FfmpegCommandBuilder builder2 = new FfmpegCommandBuilder(ffmpeg, fileVoice.getAbsolutePath());
//        File file1 = builder2.noiseFixNeuro();
        FfmpegCommandBuilder builder = new FfmpegCommandBuilder(ffmpeg, fileVoice.getAbsolutePath());
        int volume = builder.getVolume();
//        List<Segment> silencedetect = builder.silencedetect(volume);

//        File file2 = builder.cutSegment(silencedetect);

        if (type == MessageType.VOICE) {
            builder.withAudioSettings(volume);
        }
        if (type == MessageType.VIDEO) {
            builder.withVideoNoteSettings(volume);
        }

        File fileWithoutSilent = builder.execute();

        if (fileWithoutSilent == null || !fileWithoutSilent.exists()) {
            logger.error("Ffmpeg cant process file");
            return null;
        }

        FfmpegCommandBuilder builder1 = new FfmpegCommandBuilder(ffmpeg, fileWithoutSilent.getAbsolutePath());
        List<String> files = builder1.cutParam(fileWithoutSilent.getName());
        if (files.size() > 1) {
            logger.info("Cut for {} files", files.size());
        }
        deleteFile(fileWithoutSilent);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0, filesSize = files.size(); i < filesSize; i++) {
            String filePath = files.get(i);

            File file = new File(filePath);
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
                    logger.warn("Rate too much {} (count {})", rateLimiter.getRate(), countOfUse);
                    MessageValidator.sleep();
                }
                countOfUse++;
                try {
                    RequestCallback requestCallback = request -> {
                        request.getHeaders().addAll(headers);
                        request.getBody().write(bytes);
                    };
                    int finalI = i;
                    String execute = restTemplate.execute(URI.create(setting.getUrl()), HttpMethod.POST, requestCallback, clientHttpResponse -> {
                        try (InputStream inputStream = new BufferedInputStream(clientHttpResponse.getBody())) {
                            String lastText = "";
                            long lastExecutionTime = 0L;
                            JsonParser jp = jsonFactory.createParser(inputStream);
                            jp.setCodec(objectMapper);
                            jp.nextToken();
                            while (jp.currentToken() != null) {
                                if (jp.currentToken() == JsonToken.START_OBJECT) {
                                    JsonNode node = jp.readValueAsTree();
                                    String currentText = node.has("text") ? node.get("text").asText() : null;
                                    if (!StringUtils.isEmpty(currentText)) {
                                        if (node.has("is_final") && node.get("is_final").asBoolean()) {
//                                            logger.info(finalI + " " + currentText);
                                            return currentText;
                                        } else if (!currentText.equals(lastText) && node.has("speech")) {
                                            lastText = currentText;
                                            long currentTime = System.currentTimeMillis();
                                            if (currentTime - lastExecutionTime >= MIN_UPDATE_INTERVAL) {
                                                String updateMsg = stringBuilder.length() == 0 ? currentText : stringBuilder + " " + uncapitalize(currentText);
                                                Boolean result = progressCallback.apply(new RecognizeResult(updateMsg + " ⏳", null));
                                                if (!result) {
                                                    return MessageValidator.SKIP;
                                                }
                                                lastExecutionTime = currentTime;
                                            }
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
                    stringBuilder
                            .append(stringBuilder.length() == 0 ? execute : uncapitalize(execute))
                            .append(stringBuilder.length() == 0 ? "" : " ");
                } catch (Exception e) {
                    logger.error("Cant send request to wit ai [" + setting.getName() + "] ", e);
                }
            }
        }
        return stringBuilder.toString();
    }

    public static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
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
        if (isForLongOnly && duration < 40) {
            return false;
        }
        return duration < settings.get(0).getMaxSeconds();
    }

    public int priority() {
        return countOfUse;
    }
}