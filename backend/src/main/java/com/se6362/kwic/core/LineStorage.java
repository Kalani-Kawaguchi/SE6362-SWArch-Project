/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: LineStorage.java
    Description: Module to parse string containing many words into a LineList object
 */

package com.se6362.kwic.core;

public class LineStorage extends PageList
{
    // Stores a line without a uri in the LineList
    public void storeLine(String descriptor)
    {
        super.appendPage(new WebPage("", descriptor));
    }

    // Stores a line with a uri
    public void storeLine(String descriptor, String url)
    {
        super.appendPage(new WebPage(url, descriptor));
    }
}
