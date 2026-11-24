/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: CircularShift.java
    Description: Module to circular shift all lines in a LineList object
 */

package com.se6362.index;

import com.se6362.web.*;

public class CircularShifter implements KwicProcessingStep
{
    private PageList inputPages;

    public CircularShifter(PageList pages)
    {
        this.inputPages = pages;
    }

    // Takes a LineList objects and stores all circular shifts of every line
    @Override
    public PageList getOutput()
    {
        PageList outputPages = new PageList();
        for (WebPage page : inputPages)
        {
            PageList shifts = new PageList();
            for (int i = 0; i < page.getDescriptor().getLength(); i++)
            {
                shifts.append(
                        new WebPage(page.getTitle(), page.getUrl(), new ShiftedDescriptor(page.getDescriptor(), i)));
            }
            outputPages.append(shifts);
        }
        return outputPages;
    }
}
