package com.se6362.kwic.core;

public class OutputBuffer extends LineList
{
    private MergedLines outputStorage = new MergedLines();

    public synchronized void setOutputLines(Alphabetizer newList)
    {
        MergedLines temp = new MergedLines();
        temp.mergeLines(outputStorage, newList);
        outputStorage = temp;
    }

    public void writeToDatabase()
    {
        // TO DO: Save results to database
    }
    
    @Override
    public synchronized void clear()
    {
        super.clear();
        outputStorage.clear();
    }


    public String toString()
    {
        return outputStorage.toString();
    }
}
