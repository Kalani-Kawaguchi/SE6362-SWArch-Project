/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: MergedLines.java
    Description: Module to merge lines from two alphabetized LineLists into one LineList
 */

package com.se6362.kwic;

import com.se6362.webpage.*;

public class Merger implements KwicProcessingStep
{
    private PageList leftInput;
    private PageList rightInput;

    public Merger(PageList leftInput, PageList rightInput)
    {
        this.leftInput = leftInput;
        this.rightInput = rightInput;
    }

    // Merges Lines from oldList and newList into single list
    @Override
    public PageList getOutput()
    {
        PageList outputList = new PageList();
        int leftPointer = 0;
        int rightPointer = 0;
        // Same as merge operation from merge-sort
        while (leftPointer < leftInput.getPageCount() && rightPointer < rightInput.getPageCount())
        {
            if (leftInput.getPage(leftPointer).compareTo(rightInput.getPage(rightPointer)) > 0)
            {
                outputList.append(rightInput.getPage(rightPointer));
                rightPointer++;
            }
            else if (leftInput.getPage(leftPointer).compareTo(rightInput.getPage(rightPointer)) < 0)
            {
                outputList.append(leftInput.getPage(leftPointer));
                leftPointer++;
            }
            // if both are the same, only keep one.
            else
            {
                outputList.append(leftInput.getPage(leftPointer));
                leftPointer++;
                rightPointer++;
            }
        }

        while (leftPointer < leftInput.getPageCount())
        {
            outputList.append(leftInput.getPage(leftPointer));
            leftPointer++;
        }

        while (rightPointer < rightInput.getPageCount())
        {
            outputList.append(rightInput.getPage(rightPointer));
            rightPointer++;
        }
        return outputList;
    }
}
