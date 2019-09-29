package ru.chernyshev.recognizer.service.recognize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.chernyshev.recognizer.service.recognize.FfmpegCommandBuilder.Key.*;

public class FfmpegCommandBuilder {
    private static Logger logger = LoggerFactory.getLogger(FfmpegCommandBuilder.class);

    private final String ffmpegPath;
    private final String inputFilePath;
    private ProcessBuilder processBuilder;
    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String outputFilePath;

    public FfmpegCommandBuilder(String ffmpegPath, String inputFilePath) {
        this.ffmpegPath = ffmpegPath;
        this.inputFilePath = inputFilePath;
    }

    public FfmpegCommandBuilder withDefaultSettings() {
        outputFilePath = UUID.randomUUID().toString() + ".ogg";
        processBuilder = new ProcessBuilder(ffmpegPath, IN_FILE.key, inputFilePath,
                AUDIO_CODEC.key, "libvorbis",
                BIT_RATE.key, "20100",
                SAMPLING_FREQUENCY.key, "16000",
                // VBR.key, "on",
                // START_SECOND.key, "1",
                // DURATION.key, "19",
                outputFilePath);
        processBuilder.directory(new File(tmpDir));
        return this;
    }

    public File execute() {
        try {
            Process start = processBuilder.start();
            start.waitFor(1, TimeUnit.SECONDS);
        } catch (IOException e) {
            logger.error("Cant run ffmpeg process", e);
            return null;
        } catch (InterruptedException e) {
            logger.error("Ffmpeg process wait error", e);
            return null;
        }
        return new File(tmpDir, outputFilePath);
    }

    static enum Key {
        IN_FILE("-i"),
        AUDIO_CODEC("-acodec"),
        BIT_RATE("-b:a"),
        SAMPLING_FREQUENCY("-ar"),//частода дискритизации
        VBR("-vbr"),//-vbr on is default for -b:a
        START_SECOND("-ss"),
        DURATION("-t");

        private final String key;

        Key(String key) {
            this.key = key;
        }
    }
}
