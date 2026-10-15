/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: OutputBuffer.java
    Description: Stores processed lines until the system is ready to move them to the database
 */

package com.se6362.kwic.core;

import com.se6362.kwic.database.IndexDatabase;

public class OutputBuffer extends LineList
{
    private MergedLines outputStorage = new MergedLines();

    // Adds new list to the output.
    // If the buffer already contains a list of lines, new lines are merged to
    // maintain alphabetization.
    public synchronized void setOutputLines(Alphabetizer newList)
    {
        MergedLines temp = new MergedLines();
        temp.mergeLines(outputStorage, newList);
        outputStorage = temp;
    }

    // Writes all lines currently stored in the buffer to the database
    public int writeToDatabase(IndexDatabase database)
    {
        return database.replaceIndex(getOutputLines());
    }

    // Returns a LineList object containing all the output
    public synchronized LineStorage getOutputLines()
    {
        LineStorage copy = new LineStorage();
        for (int lineNo = 0; lineNo < outputStorage.getLineCount(); lineNo++)
        {
            copy.appendLine(outputStorage, lineNo);
        }
        return copy;
    }

    // Removes all lines currently store in the output buffer
    @Override
    public synchronized void clear()
    {
        super.clear();
        outputStorage.clear();
    }

    // Returns a string containing all lines separated by newline characters
    public String toString()
    {
        return outputStorage.toString();
    }
}
