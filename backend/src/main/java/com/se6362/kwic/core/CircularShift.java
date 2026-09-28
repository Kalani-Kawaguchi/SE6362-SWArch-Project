package com.se6362.kwic.core;

public class CircularShift extends LineList
{

    public void processLines(LineList lineList)
    {
        clear();

        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            this.appendLine(lineList, lineNo);
            this.genShifts(getLineCount() - 1);
        }

    }

    private void genShifts(int lineNo)
    {
        int wordCount = super.getWordCount(lineNo);
        for (int i = 1; i < wordCount; i++)
        {
            for (int j = 0; j < wordCount; j++)
            {
                super.setWord(
                        lineNo + i,
                        j,
                        super.getWord(lineNo, (i + j) % wordCount));
            }
        }
    }
}
