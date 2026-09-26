package com.se6362.kwic.core;

import java.util.Objects;

public class Alphabetizer extends LineList {
    
    private final CircularShift circularShift;

    public Alphabetizer(CircularShift circularShift) {
        this.circularShift = Objects.requireNonNull(circularShift);
    }

    public void readLines() {
        clear();

        for (int lineNumber = 0; lineNumber < circularShift.getLineCount(); lineNumber++) {
            for (int wordNumber = 0; wordNumber < circularShift.getWordCount(lineNumber); wordNumber++) {
                setWord(lineNumber, wordNumber, circularShift.getWord(lineNumber, wordNumber));
            }
        }

        alpha();
    }

    private void alpha() {
        lines.sort(LineList::compareLines);
    }
}
