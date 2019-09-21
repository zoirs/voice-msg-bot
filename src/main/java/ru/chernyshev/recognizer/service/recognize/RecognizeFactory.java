package ru.chernyshev.recognizer.service.recognize;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecognizeFactory {

    private final List<Recognizer> activeRecognizers;

    @Autowired
    public RecognizeFactory(YaSpeechKitService yaSpeechKitService,
                            DialogFlowService dialogFlowService,
                            @Value("${recognizers}") String recognizers) {

        this.activeRecognizers = new ArrayList<>();
        for (String r : recognizers.split(",")) {
            RecognizerType type = RecognizerType.valueOf(r);// todo обработать ошибки
            switch (type) {
                case YANDEX:
                    activeRecognizers.add(yaSpeechKitService);
                    break;
                case DIALOGFLOW:
                    activeRecognizers.add(dialogFlowService);
                    break;
            }
        }
    }

    public List<Recognizer> create(int duration) {
        List<Recognizer> applicableRecognizers = activeRecognizers.stream()
                .filter(r -> r.isApplicable(duration))
                .collect(Collectors.toList());
        Collections.shuffle(applicableRecognizers);
        return applicableRecognizers;
    }
}
