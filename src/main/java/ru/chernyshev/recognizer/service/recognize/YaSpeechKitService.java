package ru.chernyshev.recognizer.service.recognize;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.chernyshev.recognizer.dto.YandexMsgResponse;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.service.AimTokenService;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Component
public class YaSpeechKitService implements Recognizer {

    private static Logger logger = LoggerFactory.getLogger(YaSpeechKitService.class);

    private final RestTemplate restTemplate;
    private final String urlRecognize;
    private final String folderId;
    private final AimTokenService aimTokenService;

    @Autowired
    public YaSpeechKitService(RestTemplate restTemplate,
                              @Value("${yandex.urlRecognize}") String urlRecognize,
                              @Value("${yandex.folderId}") String folderId, AimTokenService aimTokenService) {
        this.restTemplate = restTemplate;
        this.urlRecognize = urlRecognize;
        this.folderId = folderId;
        this.aimTokenService = aimTokenService;
    }

    public String recognize(File file) {

        byte[] bytes;
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            logger.error("Cant read file " + file, e);
            return null;
        }

        String iamToken = aimTokenService.getIamToken(); // Укажите IAM-токен.
        if (StringUtils.isEmpty(iamToken)) {
            logger.error("iam token is empty");
        }
        String lang = "ru-RU";

        String query;
        try {
            query = String.format("topic=%s&lang=%s&folderId=%s",
                    "general",
                    URLEncoder.encode(lang, "UTF-8"),
                    URLEncoder.encode(folderId, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("Create query exception", e);
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(iamToken);
        HttpEntity entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<YandexMsgResponse> response = restTemplate.postForEntity(urlRecognize + query, entity, YandexMsgResponse.class);
        if (response.getStatusCodeValue() != 200) {
            logger.error("Bad response {}, {}", response.getStatusCode(), response.toString());
            return null;
        }

        YandexMsgResponse msg = response.getBody();
        return msg != null ? msg.getResult() : null;
    }

    @Override
    public RecognizerType getType() {
        return RecognizerType.YANDEX;
    }

    @Override
    public boolean isApplicable(int duration) {
        return duration < 30;
    }
}
