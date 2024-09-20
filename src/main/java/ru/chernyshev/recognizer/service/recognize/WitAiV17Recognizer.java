package ru.chernyshev.recognizer.service.recognize;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.chernyshev.recognizer.dto.WitAiChunkResponse;
import ru.chernyshev.recognizer.dto.WitAtMsgResponse;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.utils.FfmpegCommandBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WitAiV17Recognizer implements Recognizer {

    private static final Logger logger = LoggerFactory.getLogger(WitAiV17Recognizer.class);

    private final String ffmpeg;
    private final RestTemplate restTemplate;
    private final List<WitSettings> settings;
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper;

    private final RateLimiter rateLimiter;

    private int countOfUse;

    @Autowired
    public WitAiV17Recognizer(@Value("${ffmpeg.path}") String ffmpeg,
                              @Value("${WITAT_V17}") List<String> configs,
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
    public String recognize(File fileVoice) {

        File file = new FfmpegCommandBuilder(ffmpeg, fileVoice.getAbsolutePath())
                .withDefaultSettings()
                .execute();

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
            HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);
            ResponseEntity<Resource> responseEntity;
            if (!rateLimiter.tryAcquire()) {
                logger.warn("Rate too much");
            }
            countOfUse++;
            try {
                responseEntity = restTemplate.exchange(setting.getUrl(), HttpMethod.POST, entity, Resource.class);
            } catch (Exception e) {
                logger.error("Cant send request to wit ai [" + setting.getName() + "] ", e);
                continue;
            }

            if (responseEntity.getStatusCodeValue() != 200 || responseEntity.getBody() == null) {
                logger.error("Empty or error response from wit ai [{}] {}, {}", setting.getName(), responseEntity.getStatusCode(), responseEntity.getBody());
                continue;
            }

            Resource body = responseEntity.getBody();
            try {
                return parseBody(body);
            } catch (IOException e) {
                logger.error("Cant read body from wit ai [" + setting.getName() + "] ", e);
                continue;
            }
        }
        return null;
    }

    private String parseBody(Resource body) throws IOException {
        StringBuilder result = new StringBuilder();
        InputStream responseInputStream = body.getInputStream();
        JsonParser jp = jsonFactory.createParser(responseInputStream);
        jp.setCodec(objectMapper);
        jp.nextToken();
        while (jp.hasCurrentToken()) {
            WitAiChunkResponse token = jp.readValueAs(WitAiChunkResponse.class);
            jp.nextToken();
            if (token.isIs_final() && !StringUtils.isEmpty(token.getText())) {
                result.append(token.getText());
                result.append(" ");
            }
        }
        return result.toString();
    }

    private void deleteFile(File voiceFile) {
        try {
            Files.deleteIfExists(voiceFile.toPath());
        } catch (IOException e) {
            logger.warn("Cant delete file {}", voiceFile);
        }
    }

    @Override
    public RecognizerType getType() {
        return RecognizerType.WITAT_V17;
    }

    @Override
    public boolean isApplicable(int duration) {
        return duration < 19;
    }

    public int priority() {
        return countOfUse;
    }
}