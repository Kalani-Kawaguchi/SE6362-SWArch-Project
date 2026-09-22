package com.se6362.kwic.LineList;

public class CircShiftedLineList extends LineList
{
    public void processLines(LineList lineList)
    {
        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            int nextLine = super.getLineCount();
            for (int wordNo = 0; wordNo < lineList.getWordCount(lineNo); wordNo++)
            {
                super.setWord(nextLine, wordNo, lineList.getWord(lineNo, wordNo));
            }
            this.generateCircularShifts(nextLine);
        }
    }

    private void generateCircularShifts(int LineNumber)
    {
        int wordCount = super.getWordCount(LineNumber);
        for (int i = 1; i < wordCount; i++)
        {
            for (int j = 0; j < wordCount; j++)
            {
                super.setWord(
                        LineNumber + i,
                        j,
                        super.getWord(LineNumber, (i + j) % wordCount));
            }
        }
    }
}
