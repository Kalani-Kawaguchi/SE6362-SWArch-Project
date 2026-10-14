package com.se6362.kwic.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class OutputBuffer extends LineList
{
    private MergedLines outputStorage = new MergedLines();

    public synchronized void setOutputLines(Alphabetizer newList)
    {
        MergedLines temp = new MergedLines();
        temp.mergeLines(outputStorage, newList);
        outputStorage = temp;
    }

    public synchronized List<SearchResult> search(String keyword)
    {
        List<SearchResult> results = new ArrayList<>();

        for (int lineNo : outputStorage.findLinesStartingWith(keyword))
        {
            URI uri = outputStorage.getURI(lineNo);
            String url = (uri == null) ? null : uri.toString();
            results.add(new SearchResult(url, outputStorage.getLineText(lineNo)));
        }

        return results;
    }

    public synchronized void writeToDatabase()
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
