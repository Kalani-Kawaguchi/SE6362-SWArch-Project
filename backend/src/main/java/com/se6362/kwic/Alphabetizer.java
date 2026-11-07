/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: Alphabetizer.java
    Description: Module to alphabetize all lines in a LineList object
 */

package com.se6362.kwic;

import com.se6362.core.*;

public class Alphabetizer extends PageList
{
    // Takes a LineList objects and stores all lines in alphabetical order
    public void alphabetizeLines(PageList pageList)
    {
        clear();

        for (WebPage page : pageList)
        {
            appendPage(page);
        }

        aplphabetize();
    }

    // Sorts lines in alphabetical order
    private void aplphabetize()
    {
        super.sortPages();
        for (int i = 1; i < this.getPageCount(); i++)
        {
            if (this.getPage(i - 1).compareTo(this.getPage(i)) == 0)
            {
                this.deletePage(i); // because i is a duplicate
            }
        }
    }
}
