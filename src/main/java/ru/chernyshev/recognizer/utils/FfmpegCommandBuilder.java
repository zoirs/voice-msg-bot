package ru.chernyshev.recognizer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.chernyshev.recognizer.utils.FfmpegCommandBuilder.Key.*;

public class FfmpegCommandBuilder {
    private static final Logger logger = LoggerFactory.getLogger(FfmpegCommandBuilder.class);

    private final String ffmpegPath;
    private final String inputFilePath;
    private ProcessBuilder processBuilder;
    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String outputFilePath;

    public FfmpegCommandBuilder(String ffmpegPath, String inputFilePath) {
        this.ffmpegPath = ffmpegPath;
        this.inputFilePath = inputFilePath;
    }

    public FfmpegCommandBuilder withAudioSettings() {
        outputFilePath = UUID.randomUUID().toString() + ".ogg";
        processBuilder = new ProcessBuilder(ffmpegPath, IN_FILE.key, inputFilePath,
                AUDIO_CODEC.key, "libvorbis",
                BIT_RATE.key, "20100",
                SAMPLING_FREQUENCY.key, "16000",
                // VBR.key, "on",
                // START_SECOND.key, "1",
                // DURATION.key, "19",
                "-af", "silenceremove=start_periods=1:stop_periods=-1:stop_duration=0.2:start_threshold=-45dB:stop_threshold=-45dB",
                outputFilePath);
        processBuilder.directory(new File(tmpDir));
        return this;
    }

    public FfmpegCommandBuilder withVideoNoteSettings() {
        outputFilePath = UUID.randomUUID().toString() + ".ogg";
        processBuilder = new ProcessBuilder(ffmpegPath,
                IN_FILE.key, inputFilePath,
                "-codec:a", "libvorbis",
                "-qscale:a", "3",
                "-map", "0:a",
                "-async", "1",
                "-af", "silenceremove=start_periods=1:stop_periods=-1:stop_duration=0.2:start_threshold=-45dB:stop_threshold=-45dB",
                "-vn",
                outputFilePath);
        processBuilder.directory(new File(tmpDir));
        return this;
    }

    public File execute() {
        try {
            Process start = processBuilder.start();

//            BufferedReader inReader = new BufferedReader(new InputStreamReader(start.getInputStream()));
//            String lineIn;
//            while ((lineIn = inReader.readLine()) != null) {
//                logger.info("=== " + lineIn);
//            }
//            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
//            String line;
//            while ((line = errorReader.readLine()) != null) {
//                logger.info("=== " + line);
//            }

            start.waitFor(30, TimeUnit.SECONDS);
        } catch (IOException e) {
            logger.error("Cant run ffmpeg process", e);
            return null;
        } catch (InterruptedException e) {
            logger.error("Ffmpeg process wait error", e);
            return null;
        }
        return new File(tmpDir, outputFilePath);
    }

    enum Key {
        IN_FILE("-i"),
        AUDIO_CODEC("-acodec"),
        VIDEO_CODEC("-vcodec"),
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
