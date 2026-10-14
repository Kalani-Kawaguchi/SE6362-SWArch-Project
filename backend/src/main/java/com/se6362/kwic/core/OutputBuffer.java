package com.se6362.kwic.core;

import com.se6362.kwic.database.IndexDatabase;

public class OutputBuffer extends LineList
{
    private MergedLines outputStorage = new MergedLines();

    public synchronized void setOutputLines(Alphabetizer newList)
    {
        MergedLines temp = new MergedLines();
        temp.mergeLines(outputStorage, newList);
        outputStorage = temp;
    }

    public int writeToDatabase(IndexDatabase database)
    {
        return database.replaceIndex(getOutputLines());
    }

    public synchronized LineStorage getOutputLines()
    {
        LineStorage copy = new LineStorage();
        for (int lineNo = 0; lineNo < outputStorage.getLineCount(); lineNo++)
        {
            copy.appendLine(outputStorage, lineNo);
        }
        return copy;
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
