package com.se6362.kwic.core;

public class Alphabetizer extends LineList
{

    public void processLines(LineList lineList)
    {
        clear();

        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            appendLine(lineList, lineNo);
        }

        alpha();
    }

    private void alpha()
    {
        lines.sort(LineList::compareLines);
    }
}
