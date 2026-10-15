/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: MergedLines.java
    Description: Module to merge lines from two alphabetized LineLists into one LineList
 */

package com.se6362.kwic.core;

public class MergedLines extends LineList
{
    // Merges Lines from oldList and newList into single list
    public void mergeLines(LineList oldList, LineList newList)
    {
        this.clear();
        int oldListPointer = 0;
        int newListPointer = 0;
        // Same as merge operation from merge-sort
        while (oldListPointer < oldList.getLineCount() && newListPointer < newList.getLineCount())
        {
            if (compareLines(oldList.lines.get(oldListPointer), newList.lines.get(newListPointer)) > 0)
            {
                appendLine(newList, newListPointer);
                newListPointer++;
            }
            else
            {
                // if lines are duplicates, remove them unless the URIs don't match
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
