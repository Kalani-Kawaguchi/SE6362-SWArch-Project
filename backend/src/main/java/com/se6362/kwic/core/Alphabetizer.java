/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: Alphabetizer.java
    Description: Module to alphabetize all lines in a LineList object
 */

package com.se6362.kwic.core;

public class Alphabetizer extends LineList
{
    // Takes a LineList objects and stores all lines in alphabetical order
    public void alphabetizeLines(LineList lineList)
    {
        clear();

        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            appendLine(lineList, lineNo);
        }

        sortAlpha();
    }

    // Sorts lines in alphabetical order
    private void sortAlpha()
    {
        lines.sort(LineList::compareLines);
    }
}
