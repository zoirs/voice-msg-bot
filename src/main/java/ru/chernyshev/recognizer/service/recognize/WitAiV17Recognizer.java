package ru.chernyshev.recognizer.service.recognize;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WitAiV17Recognizer implements Recognizer {

    private static final Logger logger = LoggerFactory.getLogger(WitAiV17Recognizer.class);

    private final String ffmpeg;
    private final RestTemplate restTemplate;
    private final List<WitSettings> settings;

    @Autowired
    public WitAiV17Recognizer(@Value("${ffmpeg.path}") String ffmpeg,
                              @Value("${WITAT_V17}") List<String> configs,
                              Environment env,
                              RestTemplate restTemplate) {
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
            ResponseEntity<WitAtMsgResponse> response;
            try {
                response = restTemplate.postForEntity(setting.getUrl(), entity, WitAtMsgResponse.class);
            } catch (Exception e) {
                logger.error("Cant send request to wit ai [" + setting.getName() + "] ", e);
                continue;
            }

            if (response.getStatusCodeValue() != 200) {
                logger.error("Bad response [{}] {}, {}", setting.getName(), response.getStatusCode(), response.getBody());
                continue;
            }
            return response.getBody() != null ? response.getBody().get_text() : null;
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
    public RecognizerType getType() {
        return RecognizerType.WITAT_V17;
    }

    @Override
    public boolean isApplicable(int duration) {
        return duration < 20;
    }

}