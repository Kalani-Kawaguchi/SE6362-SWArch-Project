package com.se6362.kwic.service;

import com.se6362.kwic.MasterControl.MasterControl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KwicService {
    public static final int MAX_CHARACTERS = 10_000;
    public static final int MAX_LINES = 100;
    public static final int MAX_WORDS_PER_LINE = 50;
    public static final int MAX_SHIFTS = 1_000;

    public Result generate(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter at least one non-empty line of text.");
        }
        if (text.length() > MAX_CHARACTERS) {
            throw new IllegalArgumentException("Use at most 10,000 characters.");
        }
        List<String> lines = text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        if (lines.size() > MAX_LINES) {
            throw new IllegalArgumentException("Use at most 100 non-empty lines.");
        }
        int shiftCount = 0;
        for (String line : lines) {
            int wordCount = line.split("\\s+").length;
            if (wordCount > MAX_WORDS_PER_LINE) {
                throw new IllegalArgumentException("Use at most 50 words per line.");
            }
            shiftCount += wordCount;
        }
        if (shiftCount > MAX_SHIFTS) {
            throw new IllegalArgumentException("Use at most 1,000 words in total.");
        }
        List<String> shifts = MasterControl.processLines(lines);
        return new Result(shifts, lines.size(), shifts.size());
    }

    public record Result(List<String> lines, int inputLineCount, int shiftCount) {}
}
