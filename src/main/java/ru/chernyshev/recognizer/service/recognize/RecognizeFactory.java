package ru.chernyshev.recognizer.service.recognize;


import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecognizeFactory {

    private final List<Recognizer> activeRecognizers;

    @Autowired
    public RecognizeFactory(List<Recognizer> recognizerBeans,
                            @Value("${recognizers}") String recognizers) {

        this.activeRecognizers = new ArrayList<>();
        for (String r : recognizers.split(",")) {
            RecognizerType type = Enums.getIfPresent(RecognizerType.class, r).orNull();
            Preconditions.checkArgument(type != null, "Incorrect recognize type name");
            Optional<Recognizer> recognizer = recognizerBeans.stream()
                    .filter(rec -> rec.getType() == type)
                    .findFirst();

            recognizer.ifPresent(activeRecognizers::add);
        }
        Preconditions.checkArgument(!activeRecognizers.isEmpty(), "Check configuration");
    }

    public List<Recognizer> create(int duration) {
        return activeRecognizers.stream()
                .filter(r -> r.isApplicable(duration))
                .sorted(Comparator.comparingInt(Recognizer::priority))
                .collect(Collectors.toList());
    }
}
