/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: MergedLines.java
    Description: Module to merge lines from two alphabetized LineLists into one LineList
 */

package com.se6362.kwic;

import com.se6362.core.*;

public class MergedLines extends PageList
{
    // Merges Lines from oldList and newList into single list
    public void mergeLines(PageList oldList, PageList newList)
    {
        this.clear();
        int oldListPointer = 0;
        int newListPointer = 0;
        // Same as merge operation from merge-sort
        while (oldListPointer < oldList.getPageCount() && newListPointer < newList.getPageCount())
        {
            if (oldList.getPage(oldListPointer).compareTo(newList.getPage(newListPointer)) > 0)
            {
                this.appendPage(newList.getPage(newListPointer));
                newListPointer++;
            }
            else if (oldList.getPage(oldListPointer).compareTo(newList.getPage(newListPointer)) < 0)
            {
                this.appendPage(oldList.getPage(oldListPointer));
                oldListPointer++;
            }
            // if both are the same, only keep one.
            else
            {
                this.appendPage(oldList.getPage(oldListPointer));
                oldListPointer++;
                newListPointer++;
            }
        }

        while (oldListPointer < oldList.getPageCount())
        {
            this.appendPage(oldList.getPage(oldListPointer));
            oldListPointer++;
        }

        while (newListPointer < newList.getPageCount())
        {
            this.appendPage(newList.getPage(newListPointer));
            newListPointer++;
        }
    }
}
