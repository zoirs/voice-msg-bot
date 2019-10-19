package ru.chernyshev.recognizer.service.recognize;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.chernyshev.recognizer.dto.WitAtMsgResponse;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.utils.FfmpegCommandBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class WitAiRecognizer implements Recognizer {

    private static Logger logger = LoggerFactory.getLogger(WitAiRecognizer.class);

    private final String ffmpeg;
    private final RestTemplate restTemplate;
    private final String auth;
    private final String url;

    @Autowired
    public WitAiRecognizer(@Value("${ffmpeg.path}") String ffmpeg,
                           @Value("${witat.auth}") String auth,
                           @Value("${witat.url}") String url,
                           RestTemplate restTemplate) {
        this.ffmpeg = ffmpeg;
        this.auth = auth;
        this.url = url;
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
            deleteFile(file);
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth);
        headers.set("Content-Type", "audio/ogg");
        HttpEntity entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<WitAtMsgResponse> response = restTemplate.postForEntity(url, entity, WitAtMsgResponse.class);
        deleteFile(file);
        if (response.getStatusCodeValue() != 200) {
            logger.error("Bad response {}, {}", response.getStatusCode(), response.getBody());
            return null;
        }
        return response.getBody() != null ? response.getBody().get_text() : null;
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
        return RecognizerType.WITAT;
    }

    @Override
    public boolean isApplicable(int duration) {
        return duration < 20;
    }
}