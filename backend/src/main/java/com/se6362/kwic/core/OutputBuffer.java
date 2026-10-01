package com.se6362.kwic.core;

public class OutputBuffer extends LineList
{
    private MergedLines outputStorage = new MergedLines();

    public synchronized void processLines(Alphabetizer newList)
    {
        MergedLines temp = new MergedLines();
        temp.merge(outputStorage, newList);
        outputStorage = temp;
    }

    @Override
    public synchronized void clear()
    {
        super.clear();
        outputStorage.clear();
    }

    public void saveToDB()
    {
        // TO DO: Save results to database
    }

    public String toString()
    {
        return outputStorage.toString();
    }
}
