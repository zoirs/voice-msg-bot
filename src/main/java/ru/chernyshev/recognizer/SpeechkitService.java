package ru.chernyshev.recognizer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.UUID;

@Component
public class SpeechkitService {

    private static Logger logger = LoggerFactory.getLogger(SpeechkitService.class);

    private final RestTemplate restTemplate;
    private final String urlSynthesize;
    private final String urlRecognize;
    private final String folderId;
    private final AimToken aimToken;

    @Autowired
    public SpeechkitService(RestTemplate restTemplate,
                            @Value("${urlSynthesize}") String urlSynthesize,
                            @Value("${urlRecognize}") String urlRecognize,
                            @Value("${folderId}") String folderId, AimToken aimToken) {
        this.restTemplate = restTemplate;
        this.urlSynthesize = urlSynthesize;
        this.urlRecognize = urlRecognize;
        this.folderId = folderId;
        this.aimToken = aimToken;
    }

    public byte[] synthesize(String text) throws IOException {
        logger.trace("Start synthesize");

        String iamToken = aimToken.getIamToken(); // Укажите IAM-токен.

        String lang = "ru-RU";

        String query = String.format("text=%s&lang=%s&folderId=%s",
                text,
                URLEncoder.encode(lang, "UTF-8"),
                URLEncoder.encode(folderId, "UTF-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(iamToken);
        HttpEntity entity = new HttpEntity(headers);

        ResponseEntity<byte[]> responce = restTemplate.postForEntity(urlSynthesize + query, entity, byte[].class);
        File saveFile = new File(UUID.randomUUID().toString() + ".ogg");
        FileUtils.writeByteArrayToFile(saveFile, Objects.requireNonNull(responce.getBody()));

        logger.info("Finish synthesize, save {}", saveFile);
        return responce.getBody();
    }


    public String recognize(File file) throws IOException {
        logger.trace("Start recognize");

        byte[] bytes = FileUtils.readFileToByteArray(file);

        String iamToken = aimToken.getIamToken(); // Укажите IAM-токен.
        String lang = "ru-RU";

        String query = String.format("topic=%s&lang=%s&folderId=%s",
                "general",
                URLEncoder.encode(lang, "UTF-8"),
                URLEncoder.encode(folderId, "UTF-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(iamToken);
        HttpEntity entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<MsgDto> response = restTemplate.postForEntity(urlRecognize + query, entity, MsgDto.class);

        MsgDto msg = response.getBody();
        logger.trace("Finish recognize {}", msg);
        return msg!= null ? msg.getResult() : null;

    }
}
