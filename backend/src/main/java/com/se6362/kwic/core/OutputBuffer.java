/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: OutputBuffer.java
    Description: Stores processed lines until the system is ready to move them to the database
 */

package com.se6362.kwic.core;

public class OutputBuffer
{
    private MergedLines indexList = new MergedLines();
    private PageList webpageList = new PageList();

    // Adds new list to the output.
    // If the buffer already contains a list of lines, new lines are merged to
    // maintain alphabetization.
    public synchronized void setOuput(Alphabetizer newIndexEntries, PageList newWebpages)
    {
        MergedLines temp = new MergedLines();
        temp.mergeLines(indexList, newIndexEntries);
        indexList = temp;
        for (WebPage webPage : newWebpages)
        {
            this.webpageList.appendPage(webPage);
        }
    }

    // Writes all lines currently stored in the buffer to the database
    public void writeToDatabase(DatabaseManager dbManager)
    {
        dbManager.insertWebpages(webpageList);
        dbManager.insertIndexEntries(indexList);
    }

    // Returns a LineList object containing all the output
    public synchronized PageList getOutputLines()
    {
        PageList copy = new PageList();
        for (WebPage page : indexList)
        {
            copy.appendPage(page);
        }
        return copy;
    }

    // Removes all lines currently store in the output buffer
    public synchronized void clear()
    {
        indexList.clear();
    }

    // Returns a string containing all lines separated by newline characters
    public String toString()
    {
        return indexList.toString();
    }
}
