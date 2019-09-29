package ru.chernyshev.recognizer.service.recognize;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.chernyshev.recognizer.service.recognize.FfmpegCommandBuilder.Key.*;

public class FfmpegCommandBuilder {
    //    private final ProcessBuilder processBuilder;
    private final String path;
    private final String filename;
    private File tempFile;
    private ProcessBuilder processBuilder;
    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String outputFile;

    public FfmpegCommandBuilder(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public FfmpegCommandBuilder withDefaultSettings() {
        try {
            tempFile = File.createTempFile("prefix-", ".ogg");
        } catch (IOException e) {

        }
        outputFile = UUID.randomUUID().toString() + ".ogg";
        processBuilder = new ProcessBuilder(path, IN_FILE.key, filename,
                AUDIO_CODEC.key, "libvorbis",
                BIT_RATE.key, "20100",
                SAMPLING_FREQUENCY.key, "16000",
//                VBR.key, "on",
//                START_SECOND.key, "1",
//                DURATION.key, "19",
                outputFile);
        processBuilder.directory(new File(tmpDir));
        return this;
    }

    public File execute(){
        try {
            Process start = processBuilder.start();
            start.waitFor(1, TimeUnit.SECONDS);
        } catch (IOException e) {
            System.out.println(e);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        return new File(tmpDir, outputFile);
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
