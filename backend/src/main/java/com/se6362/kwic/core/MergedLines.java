package com.se6362.kwic.core;

public class MergedLines extends LineList
{
    public void mergeLines(LineList oldList, LineList newList)
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
                // if lines are duplicates
                if (compareLines(oldList.lines.get(oldListPointer), newList.lines.get(newListPointer)) == 0)
                {
                    if (oldList.getURI(oldListPointer) == null && newList.getURI(newListPointer) == null)
                    {
                        newListPointer++; // remove duplicate if URIs are null
                    }
                    else if (oldList.getURI(oldListPointer) != null && newList.getURI(newListPointer) != null &&
                            oldList.getURI(oldListPointer).toString() == newList.getURI(newListPointer).toString())
                    {
                        newListPointer++; // also remove duplicate if URIs are the same
                    }
                }
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
