package ru.chernyshev.recognizer.service.recognize;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class RecognizeFactory {

    private final YaSpeechKitService yaSpeechKitService;
    private final DialogFlowService dialogFlowService;

    @Autowired
    public RecognizeFactory(YaSpeechKitService yaSpeechKitService, DialogFlowService dialogFlowService) {
        this.yaSpeechKitService = yaSpeechKitService;
        this.dialogFlowService = dialogFlowService;
    }

    public List<Recognizer> create(int duration) {
        if (duration >= 60) {
            return Collections.emptyList();
        }
        if (duration >= 30) {
            return ImmutableList.of(dialogFlowService);
        }
        //todo переделать, не должен каждый раз создаваться список
        List<Recognizer> recognizers = Lists.newArrayList(dialogFlowService, yaSpeechKitService);
        Collections.shuffle(recognizers);
        return ImmutableList.copyOf(recognizers);
    }
}
