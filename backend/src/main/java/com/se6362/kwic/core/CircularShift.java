package com.se6362.kwic.core;

import java.util.ArrayList;
import java.util.Objects;

public class CircularShift extends LineList {
    private final LineStorage lineStorage;

    public CircularShift(LineStorage lineStorage) {
        this.lineStorage = Objects.requireNonNull(lineStorage);
    }

    public void readLines() {
        clear();

        for (int lineNumber = 0; lineNumber < lineStorage.getLineCount(); lineNumber++) {
            ArrayList<String> inputLine = new ArrayList<>();
            for (int wordNumber = 0; wordNumber < lineStorage.getWordCount(lineNumber); wordNumber++) {
                inputLine.add(lineStorage.getWord(lineNumber, wordNumber));
            }

            genShifts(inputLine);
        }
    }

    private void genShifts(ArrayList<String> inputLine) {
        int wordCount = inputLine.size();

        for (int shiftOffset = 0; shiftOffset < wordCount; shiftOffset++) {
            int shiftedLineNumber = getLineCount();
            for (int wordNumber = 0; wordNumber < wordCount; wordNumber++) {
                int sourceWordNumber = (shiftOffset + wordNumber) % wordCount;
                setWord(shiftedLineNumber, wordNumber, inputLine.get(sourceWordNumber));
            }
        }
    }
}
