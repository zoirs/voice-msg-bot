package ru.chernyshev.recognizer;

import com.google.common.base.Splitter;
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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

@Component
public class AimToken {

    private static Logger logger = LoggerFactory.getLogger(AimToken.class);

    private final RestTemplate restTemplate;
    private final GwtService gwtService;
    private final String tokensUrl;
    private LocalDateTime expiresAt;
    private String iamToken;

    @Autowired
    public AimToken(RestTemplate restTemplate, GwtService gwtService, @Value("${tokensUrl}") String tokensUrl) {
        this.restTemplate = restTemplate;
        this.gwtService = gwtService;
        this.tokensUrl = tokensUrl;
    }

    public String getIamToken() {
        if (expiresAt.isBefore(LocalDateTime.now())) {
            logger.info("Token is old. Try get new");
            createNewIamToken();
        }
        return iamToken;
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

        ResponseEntity<String> response = restTemplate.postForEntity(tokensUrl, entity, String.class);
        logger.info("New token get successfully");
        init(response.getBody());
    }

    private void init(String s) {
        s = s.substring(1, s.length() - 2);
        Map<String, String> result = Splitter.on(',')
                .trimResults()
                .withKeyValueSeparator(
                        Splitter.on('=')
                                .limit(2)
                                .trimResults())
                .split(s);

        iamToken = result.get("iamToken");
        ZonedDateTime zdt = ZonedDateTime.parse(result.get("expiresAt"));
        expiresAt = zdt.toLocalDateTime();
        logger.info("New token parsed successfully. End time {}", expiresAt);
    }
}
