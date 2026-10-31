/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: CircularShift.java
    Description: Module to circular shift all lines in a LineList object
 */

package com.se6362.kwic.core;

public class CircularShift extends PageList
{
    // Takes a LineList objects and stores all circular shifts of every line
    public void circularShiftLines(PageList pageList)
    {
        clear();

        for (WebPage page : pageList)
        {
            genShifts(page);
        }
    }

    // Generates the circular shifts for a specific line
    private void genShifts(WebPage page)
    {
        for (WebPage shift : page.getShifts())
        {
            appendPage(shift);
        }
    }
}
