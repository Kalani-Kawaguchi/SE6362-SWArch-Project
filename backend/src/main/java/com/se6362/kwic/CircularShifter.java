/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: CircularShift.java
    Description: Module to circular shift all lines in a LineList object
 */

package com.se6362.kwic;

import com.se6362.webpage.*;

public class CircularShifter implements KwicProcessingStep
{
    private PageList inputPages;

    public CircularShifter(PageList pages)
    {
        this.inputPages = pages;
    }

    // Takes a LineList objects and stores all circular shifts of every line
    public PageList getOutput()
    {
        PageList outputPages = new PageList();
        for (WebPage page : inputPages)
        {
            outputPages.append(page.getShifts());
        }
        return outputPages;
    }
}
