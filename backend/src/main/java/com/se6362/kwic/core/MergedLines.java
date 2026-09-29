package com.se6362.kwic.core;

public class MergedLines extends LineList
{
    public void merge(LineList oldList, LineList newList)
    {
        this.clear();
        int oldListPointer = 0;
        int newListPointer = 0;
        while (oldListPointer < oldList.getLineCount() && newListPointer < newList.getLineCount())
        {
            if (compareLines(oldList.lines.get(oldListPointer), newList.lines.get(newListPointer)) > 0)
            {
                appendLine(newList, newListPointer);
                newListPointer++;
            }
            else
            {
                appendLine(oldList, oldListPointer);
                oldListPointer++;
            }
        }

        while (oldListPointer < oldList.getLineCount())
        {
            appendLine(oldList, oldListPointer);
            oldListPointer++;
        }

        while (newListPointer < newList.getLineCount())
        {
            appendLine(newList, newListPointer);
            newListPointer++;
        }
    }
}
