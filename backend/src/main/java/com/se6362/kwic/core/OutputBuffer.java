package com.se6362.kwic.core;

public class OutputBuffer extends LineList
{
    private MergedLines outputBuffer = new MergedLines();

    public synchronized void processLines(Alphabetizer newList)
    {
        MergedLines temp = new MergedLines();
        temp.merge(outputBuffer, newList);
        outputBuffer = temp;
    }

    public void saveToDB()
    {
        // TO DO: Save results to database
    }
}
