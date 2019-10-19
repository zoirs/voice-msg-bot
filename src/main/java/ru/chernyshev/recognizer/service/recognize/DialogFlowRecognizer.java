package ru.chernyshev.recognizer.service.recognize;

import com.google.cloud.dialogflow.v2.*;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.model.RecognizerType;
import ru.chernyshev.recognizer.utils.EnvUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class DialogFlowRecognizer implements Recognizer {
    private static Logger logger = LoggerFactory.getLogger(DialogFlowRecognizer.class);

    private static final AudioEncoding ENCODING = AudioEncoding.AUDIO_ENCODING_OGG_OPUS;
    private static final int SAMPLE_RATE_HERTZ = 16000;
    private static final String LANGUAGE = "ru";

    private final SessionsClient sessionsClient;
    private final QueryInput queryInput;
    private final SessionName session;

    public DialogFlowRecognizer(@Value("${google.projectId}") String projectId,
                                @Value("${google.jsonFileName}") String jsonFileName) throws IOException {

        logger.info("System.getenv() {}",System.getenv().get("GOOGLE_CLOUD_PROJECT"));
        logger.info("System.getenv() {}",System.getenv().get("GOOGLE_APPLICATION_CREDENTIALS"));
        Resource settings = new FileSystemResource(jsonFileName);//todo грузить из resources
        if (!settings.exists() || !settings.isFile()) {
            logger.error("Setting not found");
            throw new IllegalStateException("");
        }
        try {
            String absolutePath = settings.getFile().getAbsolutePath();
            logger.info("absolutePath {}",absolutePath);
            Map<String, String> of = ImmutableMap.of("GOOGLE_CLOUD_PROJECT", projectId, "GOOGLE_APPLICATION_CREDENTIALS", absolutePath);
            EnvUtils.setEnv(of);
        } catch (Exception e) {

        }
        logger.info("System.getenv() {}",System.getenv().get("GOOGLE_CLOUD_PROJECT"));
        logger.info("System.getenv() {}",System.getenv().get("GOOGLE_APPLICATION_CREDENTIALS"));
        this.sessionsClient = SessionsClient.create();
        this.session = SessionName.of(System.getenv().get("GOOGLE_CLOUD_PROJECT"), UUID.randomUUID().toString());
        this.queryInput = QueryInput.newBuilder().setAudioConfig(getInputAudioConfig()).build();

    }

    public String recognize(File voiceFile) {

        byte[] inputAudio;
        try {
            inputAudio = FileUtils.readFileToByteArray(voiceFile);
        } catch (IOException e) {
            logger.error("Cant read file {}", voiceFile);
            return null;
        }

        DetectIntentRequest request = DetectIntentRequest.newBuilder()
                .setSession(session.toString())
                .setQueryInput(queryInput)
                .setInputAudio(ByteString.copyFrom(inputAudio))
                .build();

        DetectIntentResponse response;
        try {
            response = sessionsClient.detectIntent(request);
        } catch (Exception e) {
            logger.error("Recognize error dialogflow", e);
            return null;
        }

        QueryResult queryResult = response.getQueryResult();
        return queryResult.getQueryText();
    }

    @Override
    public RecognizerType getType() {
        return RecognizerType.DIALOGFLOW;
    }

    @Override
    public boolean isApplicable(int duration) {
        return duration < 60;
    }

    private InputAudioConfig getInputAudioConfig() {
        return InputAudioConfig.newBuilder()
                .setAudioEncoding(ENCODING)
                .setLanguageCode(LANGUAGE)
                .setSampleRateHertz(SAMPLE_RATE_HERTZ)
                .build();
    }
}
