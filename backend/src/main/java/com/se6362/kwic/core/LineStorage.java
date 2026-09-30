package com.se6362.kwic.core;

public class LineStorage extends LineList
{
    public void setLine(String line)
    {
        int lineNo = super.getLineCount();
        int wordNo = 0;
        String cleanedLine = line.replaceAll("[^\\p{L}\\p{N}\\p{javaWhitespace}]", "").trim();
        if (cleanedLine.isEmpty())
        {
            return;
        }

        String[] words = cleanedLine.split("\\p{javaWhitespace}+");
        for (String word : words)
        {
            super.setWord(lineNo, wordNo, word);
            wordNo++;
        }
    }
}
