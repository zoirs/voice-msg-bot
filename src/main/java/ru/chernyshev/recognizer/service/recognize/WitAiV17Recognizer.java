package ru.chernyshev.recognizer.service.recognize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.util.List;

@Service
public class WitAiV17Recognizer extends WitAiBase {

    @Autowired
    public WitAiV17Recognizer(@Value("${ffmpeg.path}") String ffmpeg,
                              @Value("${WITAT_V17}") List<String> configs,
                              Environment env,
                              RestTemplate restTemplate,
                              ObjectMapper objectMapper) {
        super(ffmpeg, configs, env, restTemplate, objectMapper);
    }

    @Override
    public RecognizerType getType() {
        return RecognizerType.WITAT_V17;
    }
}