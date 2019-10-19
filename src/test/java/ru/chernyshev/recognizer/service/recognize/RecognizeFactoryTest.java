package ru.chernyshev.recognizer.service.recognize;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import ru.chernyshev.recognizer.model.RecognizerType;

import java.io.File;
import java.util.List;

import static org.hamcrest.core.Is.is;

public class RecognizeFactoryTest {

    @Test
    public void oneRecognizerTest(){
        RecognizeFactory recognizeFactory = new RecognizeFactory(Lists.list(new TestRecognizer()), "TEST");
        List<Recognizer> recognizers = recognizeFactory.create(2);
        Assert.assertThat(recognizers.size(), is(1));
    }

    @Test
    public void twoRecognizerTest(){
        RecognizeFactory recognizeFactory = new RecognizeFactory(Lists.list(new TestRecognizer(), new FakeYaRecognizer()), "TEST");
        List<Recognizer> recognizers = recognizeFactory.create(2);
        Assert.assertThat(recognizers.size(), is(1));
    }

    @Test
    public void twoRecognizer2Test(){
        RecognizeFactory recognizeFactory = new RecognizeFactory(Lists.list(new TestRecognizer(), new FakeYaRecognizer()), "TEST,YANDEX");
        List<Recognizer> recognizers = recognizeFactory.create(2);
        Assert.assertThat(recognizers.size(), is(2));
    }

    @Test
    public void applicableTest(){
        RecognizeFactory recognizeFactory = new RecognizeFactory(Lists.list(new TestRecognizer(), new FakeYaRecognizer()), "TEST,YANDEX");
        List<Recognizer> recognizers = recognizeFactory.create(20);
        Assert.assertThat(recognizers.size(), is(1));
    }


    private static class FakeYaRecognizer implements Recognizer{

        @Override
        public String recognize(File voiceFile) {
            return null;
        }

        @Override
        public RecognizerType getType() {
            return RecognizerType.YANDEX;
        }

        @Override
        public boolean isApplicable(int duration) {
            return duration < 10;
        }
    }
}
