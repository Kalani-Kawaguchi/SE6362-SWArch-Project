/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: LineStorage.java
    Description: Module to parse string containing many words into a LineList object
 */

package com.se6362.kwic.core;

import java.net.URI;

public class LineStorage extends LineList
{
    // Stores a line without a uri in the LineList
    public void storeLine(String line)
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

    // Stores a line with a uri
    public void storeLine(String line, URI uri)
    {
        int lineNo = getLineCount();
        storeLine(line);
        if (getLineCount() > lineNo)
        {
            setURI(lineNo, uri);
        }
    }
}
