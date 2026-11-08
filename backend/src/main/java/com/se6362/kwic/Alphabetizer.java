/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: Alphabetizer.java
    Description: Module to alphabetize all lines in a LineList object
 */

package com.se6362.kwic;

import com.se6362.webpage.*;

public class Alphabetizer implements KwicProcessingStep
{
    private PageList inputPages;

    public Alphabetizer(PageList pages)
    {
        this.inputPages = pages;
    }

    // Takes a LineList objects and stores all circular shifts of every line
    public PageList getOutput()
    {
        PageList outputPages = new PageList();
        outputPages.append(inputPages);
        outputPages.sortPages();

        // loop through and remove duplicates
        for (int i = 1; i < outputPages.getPageCount(); i++)
        {
            if (outputPages.getPage(i - 1).compareTo(outputPages.getPage(i)) == 0)
            {
                outputPages.deletePage(i); // because i is a duplicate
            }
        }
        return outputPages;
    }
}
