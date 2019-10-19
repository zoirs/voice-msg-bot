package ru.chernyshev.recognizer.service;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.chernyshev.recognizer.model.AimToken;

@Component
public class AimTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AimTokenService.class);

    private final RestTemplate restTemplate;
    private final GwtService gwtService;
    private final String tokensUrl;
    private AimToken iamToken;

    @Autowired
    public AimTokenService(RestTemplate restTemplate, GwtService gwtService, @Value("${yandex.tokensUrl}") String tokensUrl) {
        this.restTemplate = restTemplate;
        this.gwtService = gwtService;
        this.tokensUrl = tokensUrl;
    }

    public String getIamToken() {
        if (iamToken == null || !iamToken.isActual()) {
            logger.info("Token is old. Try get new");
            createNewIamToken();
        }
        return iamToken.getIamToken();
    }

    private void createNewIamToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String gwtToken = gwtService.getGWTToken();
        if (StringUtils.isEmpty(gwtToken)) {
            logger.error("Jwt token is empty");
            return;
        }
        ImmutableMap<String, String> of = ImmutableMap.of("jwt", gwtToken);
        HttpEntity entity = new HttpEntity<>(of, headers);

        ResponseEntity<AimToken> response = restTemplate.postForEntity(tokensUrl, entity, AimToken.class);
        if (response.getStatusCodeValue() != 200) {
            logger.error("Bad response {}, {}", response.getStatusCode(), response.toString());
            return;
        }
        iamToken = response.getBody();
        logger.info("New token get successfully");
    }
}
