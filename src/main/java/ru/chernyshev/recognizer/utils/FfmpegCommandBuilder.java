package ru.chernyshev.recognizer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.chernyshev.recognizer.dto.Segment;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                "-qscale:a", "3",
                // START_SECOND.key, "1",
                // DURATION.key, "19",
                "-af", String.format("silenceremove=start_periods=1:stop_periods=-1:stop_duration=0.5:start_threshold=%sdB:stop_threshold=%sdB", silentBorder, silentBorder),
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
                "-af", String.format("silenceremove=start_periods=1:stop_periods=-1:stop_duration=0.5:start_threshold=%sdB:stop_threshold=%sdB", silentBorder, silentBorder),
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

//    public List<Segment> silencedetect(int meanVolume) {
//        try {
//            int silentBorder = meanVolume - 10;
//
//            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,
//                    IN_FILE.key, inputFilePath,
//                    "-af", String.format("silencedetect=n=%sdB:d=0.5", silentBorder),
//                    "-f", "null", "-"
//            );
//
//            Process start = processBuilder.start();
//            String line;
//
////            BufferedReader inReader = new BufferedReader(new InputStreamReader(start.getInputStream()));
////            while ((line = inReader.readLine()) != null) {
////                logger.info("== {}", line);
////                if (line.contains("mean_volume")) {
////                    return parse(line);
////                }
////            }
//
//            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
//            List<String> lines = new ArrayList<>();
//            while ((line = errorReader.readLine()) != null) {
//                logger.info(line);
//                lines.add(line);
////                if (line.contains("mean_volume")) {
////                    return parse(line);
////                }
//            }
//            List<Segment> segments = parseSilenceLog(lines);
//            logger.info("qqq {}", segments);
//            start.waitFor(30, TimeUnit.SECONDS);
//            return segments;
//        } catch (IOException e) {
//            logger.error("Cant run ffmpeg process", e);
//            return null;
//        } catch (InterruptedException e) {
//            logger.error("Ffmpeg process wait error", e);
//            return null;
//        }
////        return null;
//    }

    public List<String> cutParam(String name) {
        try {
            String outputFilePattern = tmpDir + name + "_%03d.ogg";

// ./ffmpeg.exe -i ../../testFiles/file30.tmp -f segment -segment_time 5 output_%03d.ogg
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,
                    IN_FILE.key, inputFilePath,
                    "-f", "segment",
                    "-segment_time", "19", outputFilePattern
            );

            Process start = processBuilder.start();
            String line;


            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
            List<String> lines = new ArrayList<>();
            List<String> createdFiles = new ArrayList<>();

            while ((line = errorReader.readLine()) != null) {
                logger.info(line);
//                lines.add(line);
//                if (line.contains("mean_volume")) {
//                    return parse(line);
//                }
                // Логируем строки вывода
//                System.out.println(line);

                // Ищем строки с именами выходных файлов, используя регулярное выражение
                Pattern pattern = Pattern.compile("Opening '(.*?)' for writing");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // Извлекаем имя файла и добавляем в список
                    String outputFileName = matcher.group(1);
                    createdFiles.add(outputFileName);
                }
            }
//            logger.info("qqq {}", createdFiles);
//            List<Segment> segments = parseSilenceLog(lines);
//            logger.info("qqq {}", segments);
            processBuilder.directory(new File(tmpDir));

            start.waitFor(30, TimeUnit.SECONDS);
            return createdFiles;
        } catch (IOException e) {
            logger.error("Cant run ffmpeg process", e);
            return Collections.emptyList();
        } catch (InterruptedException e) {
            logger.error("Ffmpeg process wait error", e);
            return Collections.emptyList();
        }
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

//    private static List<Segment> parseSilenceLog(List<String> lines) {
//        List<Segment> segments = new ArrayList<>();
//        double silenceStart = -1; // Начало тишины, будем сохранять до нахождения конца
//        Pattern startPattern = Pattern.compile("\\[silencedetect @ [^\\]]+\\] silence_start: (\\d+\\.\\d+)");
//        Pattern endPattern = Pattern.compile("\\[silencedetect @ [^\\]]+\\] silence_end: (\\d+\\.\\d+)");
//
//        for (String line : lines) {
//            // Ищем начало тишины
//            Matcher startMatcher = startPattern.matcher(line);
//            if (startMatcher.find()) {
//                silenceStart = Double.parseDouble(startMatcher.group(1));
//            }
//
//            // Ищем конец тишины
//            Matcher endMatcher = endPattern.matcher(line);
//            if (endMatcher.find() && silenceStart != -1) {
//                double silenceEnd = Double.parseDouble(endMatcher.group(1));
//                segments.add(new Segment(silenceStart, silenceEnd));
//                silenceStart = -1; // Сбросим начало для следующего сегмента
//            }
//        }
//        return segments;
//    }

    //[silencedetect @ 000001b33f9f2180] silence_start: 0.12
//    private static List<List<Segment>> splitIntoFiles(List<Segment> silenceSegments, double maxFileDuration) {
//        List<List<Segment>> fileSegments = new ArrayList<>();
//        List<Segment> currentFile = new ArrayList<>();
//        double currentDuration = 0;
//
//        for (Segment segment : silenceSegments) {
//            // Проверяем, можно ли добавить этот сегмент в текущий файл
//            if (currentDuration + segment.getDuration() <= maxFileDuration) {
//                currentFile.add(segment);
//                currentDuration += segment.getDuration();
//            } else {
//                // Если нет, сохраняем текущий файл и начинаем новый
//                fileSegments.add(new ArrayList<>(currentFile));
//                currentFile.clear();
//                currentFile.add(segment);
//                currentDuration = segment.getDuration();
//            }
//        }
//
//        // Добавляем последний файл, если он не пустой
//        if (!currentFile.isEmpty()) {
//            fileSegments.add(currentFile);
//        }
//
//        return fileSegments;
//    }

//    public static String buildCommand(List<Segment> silenceSegments, double totalDuration) {
//        StringBuilder filterComplex = new StringBuilder();
//        int segmentCount = silenceSegments.size();
//        DecimalFormat decimalFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
//        double lastEnd = 0.0;
//        int index = 0;
//
//        for (Segment silence : silenceSegments) {
//            if (silence.getStart() > lastEnd) {
//                filterComplex.append(String.format("[0]atrim=start=%s:end=%s[a%d];", decimalFormat.format(lastEnd), decimalFormat.format(silence.getStart()), index++));
//            }
//            lastEnd = silence.getEnd();
//        }
//
//        if (lastEnd < totalDuration) {
//            filterComplex.append(String.format("[0]atrim=start=%s:end=%s[a%d];", decimalFormat.format(lastEnd), decimalFormat.format(totalDuration), index++));
//        }
//
//        StringBuilder concatCommand = new StringBuilder();
//        for (int i = 0; i < index; i++) {
//            concatCommand.append("[").append("a").append(i).append("]");
//        }
//        concatCommand.append("concat=n=").append(index).append(":v=0:a=1[out]");
//
//        // Добавляем concat команду к основной строке
//        filterComplex.append(concatCommand);
//
//        return filterComplex.toString();
//    }

//    public File cutSegment(List<Segment> segments) {
//        DecimalFormat df = new DecimalFormat("#.00",new DecimalFormatSymbols(Locale.US));
////        String q1 = "";
////        String q = "";
////        for (int i = 0; i < segments.size(); i++) {
////            q1 =  q1 + String.format("[0]atrim=start=%s:end=%s[a%d];", df.format(segments.get(i).getStart()), df.format(segments.get(i).getEnd()), i);
////            q = q + String.format("[a%d]", i);
////        }
////        String res = q1+q+String.format("concat=n=%d:v=0:a=1[out]", segments.size());
//        String res = buildCommand(segments, 120);
//        outputFilePath = UUID.randomUUID().toString() + ".ogg";
//
//        String s = outputFilePath + ".cut.ogg";
//        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,
//                IN_FILE.key, inputFilePath,
//                "-filter_complex", "\"" +res + "\"",
//                "-map", "\"[out]\"", s
//        );
//        processBuilder.directory(new File(tmpDir));
//
//        try {
//            Process start = processBuilder.start();
//            String line;
//
//            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
//
//            while ((line = errorReader.readLine()) != null) {
//                logger.info(line);
////                lines.add(line);
////                if (line.contains("mean_volume")) {
////                    return parse(line);
////                }
//                // Логируем строки вывода
////                System.out.println(line);
//
//                // Ищем строки с именами выходных файлов, используя регулярное выражение
//            }
////            List<Segment> segments = parseSilenceLog(lines);
////            logger.info("qqq {}", segments);
//
//            start.waitFor(30, TimeUnit.SECONDS);
//            return new File(tmpDir, s);
//
//        } catch (IOException e) {
//
//        } catch (InterruptedException e) {
//
//        }
//
//        return null;
//    }

//    public File noiseFix() {
//        outputFilePath = UUID.randomUUID().toString() + ".ogg";
//
//        String s = outputFilePath + ".noise.fix.ogg";
//        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,
//                IN_FILE.key, inputFilePath,
//                "-af", "\"" +"loudnorm" + "\"",
//                s
//        );
//        processBuilder.directory(new File(tmpDir));
//
//        try {
//            Process start = processBuilder.start();
//            String line;
//
//            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
//
//            while ((line = errorReader.readLine()) != null) {
//                logger.info(line);
//            }
//
//            start.waitFor(30, TimeUnit.SECONDS);
//            return new File(tmpDir, s);
//
//        } catch (IOException e) {
//
//        } catch (InterruptedException e) {
//
//        }
//        return null;
//    }

//    public File noiseFixNeuro() {
//        outputFilePath = UUID.randomUUID().toString() + ".ogg";
//
//        String s = outputFilePath + ".noise.fix.neuro.ogg";
//        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath,
//                IN_FILE.key, inputFilePath,
////                "-af", "arnndn=m=\"../ffmpeg-2025-01-22-full_build/rnnoise-models-master/leavened-quisling-2018-08-31/lq.rnnn\"", // 1348
////                "-af", "arnndn=m=\"../ffmpeg-2025-01-22-full_build/rnnoise-models-master/conjoined-burgers-2018-08-28/cb.rnnn\"", // 1316
//                "-af", "arnndn=m=\"../ffmpeg-2025-01-22-full_build/rnnoise-models-master/somnolent-hogwash-2018-09-01/sh.rnnn\"", // 1312
////                "-af", "arnndn=m=\"../ffmpeg-2025-01-22-full_build/rnnoise-models-master/beguiling-drafter-2018-08-30/bd.rnnn\"",   //1385
////                "-af", "arnndn=m=\"../ffmpeg-2025-01-22-full_build/rnnoise-models-master/marathon-prescription-2018-08-29/mp.rnnn\"", //1050
//                s
//        );
//        processBuilder.directory(new File("d:/Project/Java/Workspace/yandex-speechkit/testFiles/"));
//
//        try {
//            Process start = processBuilder.start();
//            String line;
//
//            BufferedReader errorReader = new BufferedReader(new InputStreamReader(start.getErrorStream()));
//
//            while ((line = errorReader.readLine()) != null) {
//                logger.info(line);
//            }
//
//            start.waitFor(30, TimeUnit.SECONDS);
//            return new File(tmpDir, s);
//
//        } catch (IOException e) {
//
//        } catch (InterruptedException e) {
//
//        }
//        return null;
//    }


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
