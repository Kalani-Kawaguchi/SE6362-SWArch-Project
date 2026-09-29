package com.se6362.kwic.core;

public class CircularShift extends LineList {

    public void processLines(LineList lineList) {
        clear();

        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++) {
            genShifts(lineList, lineNo);
        }

    }

    private void genShifts(LineList lineList, int lineNo) {
        int wordCount = lineList.getWordCount(lineNo);
        for (int offset = 0; offset < wordCount; offset++) {
            int shiftedLineNumber = getLineCount();
            for (int wordNo = 0; wordNo < wordCount; wordNo++) {
                setWord(
                        shiftedLineNumber,
                        wordNo,
                        lineList.getWord(lineNo, (offset + wordNo) % wordCount));
            }
        }
    }
}
