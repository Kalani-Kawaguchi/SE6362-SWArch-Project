package com.se6362.kwic.core;

import java.util.HashSet;
import java.util.Set;

public class CircularShift extends LineList
{

    public void circularShiftLines(LineList lineList)
    {
        clear();

        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            genShifts(lineList, lineNo);
        }
    }

    private void genShifts(LineList lineList, int lineNo)
    {
        int wordCount = lineList.getWordCount(lineNo);

        // Track the unique shifts generated for THIS specific original line
        Set<String> seenShiftsForLine = new HashSet<>();

        for (int offset = 0; offset < wordCount; offset++)
        {
            // Build a string representation of the current shift to check for duplicates
            StringBuilder shiftSignature = new StringBuilder();
            for (int wordNo = 0; wordNo < wordCount; wordNo++)
            {
                String word = lineList.getWord(lineNo, (offset + wordNo) % wordCount);
                shiftSignature.append(word).append(" ");
            }
            String signature = shiftSignature.toString().trim();

            // Only add the shift to LineList if we haven't encountered this exact shift
            // sequence yet
            if (seenShiftsForLine.add(signature))
            {
                int shiftedLineNumber = getLineCount();
                for (int wordNo = 0; wordNo < wordCount; wordNo++)
                {
                    setWord(
                            shiftedLineNumber,
                            wordNo,
                            lineList.getWord(lineNo, (offset + wordNo) % wordCount));
                }
                setURI(shiftedLineNumber, lineList.getURI(lineNo));
            }
        }
    }
}
