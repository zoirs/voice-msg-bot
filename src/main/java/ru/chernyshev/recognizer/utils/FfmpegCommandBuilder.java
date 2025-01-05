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
    private static final int DEFAULT_VOLUME = -45;
    private static final String VOLUME_PATTERN = "mean_volume:\\s(-?\\d+\\.\\d*)";

    private final String ffmpegPath;
    private final String inputFilePath;
    private ProcessBuilder processBuilder;
    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String outputFilePath;

    public FfmpegCommandBuilder(String ffmpegPath, String inputFilePath) {
        this.ffmpegPath = ffmpegPath;
        this.inputFilePath = inputFilePath;
    }

    public FfmpegCommandBuilder withAudioSettings(int meanVolume) {
        int silentBorder = meanVolume - 10;
        outputFilePath = UUID.randomUUID().toString() + ".ogg";
        processBuilder = new ProcessBuilder(ffmpegPath, IN_FILE.key, inputFilePath,
                AUDIO_CODEC.key, "libvorbis",
                BIT_RATE.key, "20100",
                SAMPLING_FREQUENCY.key, "16000",
                // VBR.key, "on",
                // START_SECOND.key, "1",
                // DURATION.key, "19",
                "-af", String.format("silenceremove=start_periods=1:stop_periods=-1:stop_duration=0.2:start_threshold=%sdB:stop_threshold=%sdB", silentBorder, silentBorder),
                outputFilePath);
        processBuilder.directory(new File(tmpDir));
        return this;
    }

    public FfmpegCommandBuilder withVideoNoteSettings(int meanVolume) {
        int silentBorder = meanVolume - 10;
        outputFilePath = UUID.randomUUID().toString() + ".ogg";
        processBuilder = new ProcessBuilder(ffmpegPath,
                IN_FILE.key, inputFilePath,
                "-codec:a", "libvorbis",
                "-qscale:a", "3",
                "-map", "0:a",
                "-async", "1",
                "-af", String.format("silenceremove=start_periods=1:stop_periods=-1:stop_duration=0.2:start_threshold=%sdB:stop_threshold=%sdB", silentBorder, silentBorder),
                "-vn",
                outputFilePath);
        processBuilder.directory(new File(tmpDir));
        return this;
    }

    public File execute() {
        try {
            Process start = processBuilder.start();
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

    public int getVolume() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,
                    IN_FILE.key, inputFilePath,
                    "-af", "volumedetect",
                    "-f", "null", "-");

            Process start = processBuilder.start();
            String line;

//            BufferedReader inReader = new BufferedReader(new InputStreamReader(start.getInputStream()));
//            while ((line = inReader.readLine()) != null) {
//                logger.info("== {}", line);
//                if (line.contains("mean_volume")) {
//                    return parse(line);
//                }
//            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                if (line.contains("mean_volume")) {
                    return parse(line);
                }
            }

            start.waitFor(30, TimeUnit.SECONDS);
        } catch (IOException e) {
            logger.error("Cant run ffmpeg process", e);
            return DEFAULT_VOLUME;
        } catch (InterruptedException e) {
            logger.error("Ffmpeg process wait error", e);
            return DEFAULT_VOLUME;
        }
        return DEFAULT_VOLUME;
    }

    private static int parse(String logLine) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(VOLUME_PATTERN);
        java.util.regex.Matcher matcher = pattern.matcher(logLine);

        if (matcher.find()) {
            double meanVolume = Double.parseDouble(matcher.group(1));
            return (int) meanVolume;
        } else {
            logger.error("Mean Volume not found in {}!", logLine);
        }
        return DEFAULT_VOLUME;
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
