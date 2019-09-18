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
import ru.chernyshev.recognizer.model.MsgDto;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.service.AimTokenService;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.UUID;

@Component
public class YaSpeechKitService implements Recognizer {

    private static Logger logger = LoggerFactory.getLogger(YaSpeechKitService.class);

    private final RestTemplate restTemplate;
    private final String urlSynthesize;
    private final String urlRecognize;
    private final String folderId;
    private final AimTokenService aimTokenService;

    @Autowired
    public YaSpeechKitService(RestTemplate restTemplate,
                              @Value("${urlSynthesize}") String urlSynthesize,
                              @Value("${urlRecognize}") String urlRecognize,
                              @Value("${folderId}") String folderId, AimTokenService aimTokenService) {
        this.restTemplate = restTemplate;
        this.urlSynthesize = urlSynthesize;
        this.urlRecognize = urlRecognize;
        this.folderId = folderId;
        this.aimTokenService = aimTokenService;
    }

    public String recognize(File file) throws IOException {
        logger.info("Start recognize");

        byte[] bytes = FileUtils.readFileToByteArray(file);

        String iamToken = aimTokenService.getIamToken(); // Укажите IAM-токен.
        if (StringUtils.isEmpty(iamToken)) {
            logger.error("iam token is empty");
        }
        String lang = "ru-RU";

        String query = String.format("topic=%s&lang=%s&folderId=%s",
                "general",
                URLEncoder.encode(lang, "UTF-8"),
                URLEncoder.encode(folderId, "UTF-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(iamToken);
        HttpEntity entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<MsgDto> response = restTemplate.postForEntity(urlRecognize + query, entity, MsgDto.class);
        if (response.getStatusCodeValue() != 200) {
            logger.error("Bad response {}, {}", response.getStatusCode(), response.toString());
            return null;
        }

        MsgDto msg = response.getBody();
        logger.info("Recognize yandex {}", msg != null ? msg.getResult() : null);
        return msg != null ? msg.getResult() : null;
    }

    @Override
    public RecognizerType getType() {
        return RecognizerType.YANDEX;
    }

    private byte[] synthesize(String text) throws IOException {
        logger.info("Start synthesize");

        String iamToken = aimTokenService.getIamToken(); // Укажите IAM-токен.

        String lang = "ru-RU";

        String query = String.format("text=%s&lang=%s&folderId=%s",
                text,
                URLEncoder.encode(lang, "UTF-8"),
                URLEncoder.encode(folderId, "UTF-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(iamToken);
        HttpEntity entity = new HttpEntity(headers);

        ResponseEntity<byte[]> responce = restTemplate.postForEntity(urlSynthesize + query, entity, byte[].class);
        if (responce.getStatusCodeValue() != 200) {
            logger.error("Bad response {}, {}", responce.getStatusCode(), responce.toString());
            return null;
        }
        File saveFile = new File(UUID.randomUUID().toString() + ".ogg");
        FileUtils.writeByteArrayToFile(saveFile, Objects.requireNonNull(responce.getBody()));

        logger.info("Finish synthesize, save {}", saveFile);
        return responce.getBody();
    }
}
