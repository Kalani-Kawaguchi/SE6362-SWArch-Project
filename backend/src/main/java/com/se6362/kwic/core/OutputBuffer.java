package com.se6362.kwic.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OutputBuffer extends LineList {
    private Alphabetizer alphabetizer;

    public OutputBuffer(Alphabetizer alphabetizer) {
        this.alphabetizer = Objects.requireNonNull(alphabetizer);
    }

    public void readLines() {
        ArrayList<ArrayList<String>> newLines = new ArrayList<>();

        for (int lineNumber = 0; lineNumber < alphabetizer.getLineCount(); lineNumber++) {
            ArrayList<String> line = new ArrayList<>();
            for (int wordNumber = 0; wordNumber < alphabetizer.getWordCount(lineNumber); wordNumber++) {
                line.add(alphabetizer.getWord(lineNumber, wordNumber));
            }
            newLines.add(line);
        }

        merge(newLines);
    }

    private void merge(ArrayList<ArrayList<String>> newLines) {
        ArrayList<ArrayList<String>> mergedLines = new ArrayList<>();
        int oldLineNumber = 0;
        int newLineNumber = 0;

        while (oldLineNumber < lines.size() && newLineNumber < newLines.size()) {
            if (compareLines(lines.get(oldLineNumber), newLines.get(newLineNumber)) <= 0) {
                mergedLines.add(lines.get(oldLineNumber));
                oldLineNumber++;
            } else {
                mergedLines.add(newLines.get(newLineNumber));
                newLineNumber++;
            }
        }

        while (oldLineNumber < lines.size()) {
            mergedLines.add(lines.get(oldLineNumber));
            oldLineNumber++;
        }

        while (newLineNumber < newLines.size()) {
            mergedLines.add(newLines.get(newLineNumber));
            newLineNumber++;
        }

        lines = mergedLines;
    }

    public List<String> getLines() {
        ArrayList<String> output = new ArrayList<>();

        for (ArrayList<String> line : lines) {
            output.add(String.join(" ", line));
        }

        return List.copyOf(output);
    }
}
