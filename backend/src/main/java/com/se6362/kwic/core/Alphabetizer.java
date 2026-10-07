package com.se6362.kwic.core;

public class Alphabetizer extends LineList
{

    public void alphabetizeLines(LineList lineList)
    {
        clear();

        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            appendLine(lineList, lineNo);
        }

        sortAlpha();
    }

    private void sortAlpha()
    {
        lines.sort(LineList::compareLines);
    }
}
