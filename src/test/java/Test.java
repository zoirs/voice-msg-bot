import com.google.common.collect.ImmutableMap;
import ru.chernyshev.recognizer.service.recognize.DialogFlowService;
import ru.chernyshev.recognizer.utils.EnvUtils;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Test {

    protected static String PROJECT_ID = System.getenv().get("GOOGLE_CLOUD_PROJECT");
    protected static String SESSION_ID = UUID.randomUUID().toString();

    @org.junit.Test
    public void testDetectIntentAudio() throws Exception {
        System.err.println(System.getProperty("sun.arch.data.model"));
        System.err.println(System.getProperty("os.arch"));
        EnvUtils.setEnv(ImmutableMap.of("SOMESOEM", "SSSSS"));
//        DialogFlowService dialogFlowService = new DialogFlowService();
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        dialogFlowService.sent();
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        dialogFlowService.sent();
//        dialogFlowService.sent();
//        dialogFlowService.sent();
//        dialogFlowService.sent();
//        dialogFlowService.sent(voiceFile);
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        System.err.println("===========================================");
//        System.err.println("=========================================== " + PROJECT_ID);
//        System.err.println(System.getenv().get("GOOGLE_APPLICATION_CREDENTIALS"));
//        List<String> askedQuestions = Lists.newArrayList();
//        com.google.cloud.dialogflow.v2.QueryResult result = DetectIntentAudio.detectIntentAudio(
//                PROJECT_ID, "C:\\Denis\\project\\yandex-speechkit\\yandex-speechkit\\src\\main\\resources\\ru16.wav", SESSION_ID, "ru");
////        PROJECT_ID, "resources/book_a_room.wav", SESSION_ID, LANGUAGE_CODE);
//
//        System.err.println(result.getQueryText());
//        System.err.println("====================");
//        System.err.format("Query Text: '%s'\n", result.getQueryText());
//        System.err.format("Detected Intent: %s (confidence: %f)\n",
//                result.getIntent().getDisplayName(), result.getIntentDetectionConfidence());
//        System.err.format("Fulfillment Text: '%s'\n", result.getFulfillmentText());

//        String fulfillmentText = result.getFulfillmentText();
//        while (!result.getAllRequiredParamsPresent()
//                && ANSWERS.containsKey(fulfillmentText)
//                && !askedQuestions.contains(fulfillmentText)) {
//            askedQuestions.add(result.getFulfillmentText());
//            assertEquals("room.reservation", result.getAction());
//            assertThat(QUESTIONS).contains(fulfillmentText);
//            result = DetectIntentAudio.detectIntentAudio(
//                    PROJECT_ID, ANSWERS.get(fulfillmentText), SESSION_ID, LANGUAGE_CODE);
//            fulfillmentText = result.getFulfillmentText();
//        }
//        assertTrue(result.getAllRequiredParamsPresent());
//        assertEquals("Choose a room please.", fulfillmentText);
    }
}
