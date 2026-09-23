package com.se6362.kwic.LineList;

public class StorageLineList extends LineList
{
    public void addLine(String line)
    {
        int lineNo = super.getLineCount();
        int wordNo = 0;
        String[] words = line.split("\\s+");
        for (String word : words)
        {
            super.setWord(lineNo, wordNo, word);
            wordNo++;
        }
    }
}
