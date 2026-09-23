package com.se6362.kwic.LineList;

public class CircShiftedLineList extends LineList
{
    public CircShiftedLineList(LineList lineList)
    {
        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            this.appendLine(lineList, lineNo);
            this.generateCircularShifts(getLineCount() - 1);
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
