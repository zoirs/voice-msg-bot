package ru.chernyshev.recognizer.dto;

public class Segment {
    private double start;
    private double end;

    public Segment(double start, double end) {
        this.start = start;
        this.end = end;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public double getDuration() {
        return end - start;
    }

    @Override
    public String toString() {
        return String.format("Start: %.2f, End: %.2f, Duration: %.2f", start, end, getDuration());
    }
}