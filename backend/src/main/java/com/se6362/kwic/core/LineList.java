/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: LineList.java
    Description: Collection for Lines objects with operations to get and set individual words
 */

package com.se6362.kwic.core;

import java.util.ArrayList;
import java.net.URI;

public abstract class LineList
{
    protected ArrayList<Line> lines = new ArrayList<>();

    // Gets the URI associated with a specific line
    public URI getURI(int lineNumber)
    {
        try
        {
            if (lineNumber >= lines.size())
            {
                throw new IllegalArgumentException("URI with this index does not exist.");
            }
            return lines.get(lineNumber).getURI();
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Illegal argument: " + e.getMessage());
            return null;
        }
    }

    // Sets the URI associated with a specific line
    protected void setURI(int lineNumber, URI uri)
    {
        try
        {
            if (lineNumber >= lines.size())
            {
                throw new IllegalArgumentException("URI must be associated with an existing line.");
            }
            lines.get(lineNumber).setURI(uri);
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Illegal argument: " + e.getMessage());
        }
    }

    // Gets a word at a specific position in a specific line
    public String getWord(int lineNumber, int wordNumber)
    {
        return lines.get(lineNumber).getWord(wordNumber);
    }

    // Sets a word at a specific position in a specific line
    protected void setWord(int lineNumber, int wordNumber, String word)
    {
        try
        {
            if (word == null || word.isBlank())
            {
                throw new IllegalArgumentException("Blank string not allowed.");
            }
            // add a new line of lineNumber is one greater than the position of the last
            // line
            if (lineNumber == lines.size() && wordNumber == 0)
            {
                lines.add(new Line());
            }
            // don't allow a line to be added at a position greater than the the position
            // after last
            else if (lineNumber >= lines.size())
            {
                throw new IllegalArgumentException("Cannot set words beyond (n + 1)-th position.");
            }
            lines.get(lineNumber).setWord(wordNumber, word);

        }
        catch (IndexOutOfBoundsException e)
        {
            System.err.println("Unable to set word at line " + lineNumber + " word " + wordNumber +
                    ". New words can only be appended to the end of a line, and new lines can only be appended to the end of the line list.");
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Unable to set word because word is blank." +
                    "Words must contain at least one non-blank character.");
        }
    }

    // Copies a line from another LineList object to this one.
    // This method is not mentioned in the architectural diagram, but it
    // helps to reduce code duplication since the operation of copying a whole line
    // happens frequently.
    protected void appendLine(LineList lineList, int lineNo)
    {
        int nextLine = this.getLineCount();
        for (int wordNo = 0; wordNo < lineList.getWordCount(lineNo); wordNo++)
        {
            this.setWord(nextLine, wordNo, lineList.getWord(lineNo, wordNo));
        }
        this.setURI(nextLine, lineList.getURI(lineNo));
    }

    // Gets the number of words in a specific line
    public int getWordCount(int lineNumber)
    {
        return lines.get(lineNumber).getWordCount();
    }

    // Gets the total number of lines
    public int getLineCount()
    {
        return lines.size();
    }

    // Removes all lines (mostly used for testing)
    public void clear()
    {
        lines.clear();
    }

    // returns 0 if they are the same, negative if leftLine is less than rightLine,
    // positive if leftLine is greater
    protected static int compareLines(Line leftLine, Line rightLine)
    {
        int wordCount = Math.min(leftLine.getWordCount(), rightLine.getWordCount());

        for (int wordNumber = 0; wordNumber < wordCount; wordNumber++)
        {
            int comparison = leftLine.getWord(wordNumber).compareToIgnoreCase(rightLine.getWord(wordNumber));
            if (comparison != 0)
            {
                return comparison;
            }
        }
        return Integer.compare(leftLine.getWordCount(), rightLine.getWordCount());
    }

    // Returns the entire line as a string where each word is separated by a space
    public String toString()
    {
        StringBuilder output = new StringBuilder();
        for (Line line : lines)
        {
            for (int i = 0; i < line.getWordCount(); i++)
            {
                output.append(line.getWord(i)).append(" ");
            }
            output.append("\n");
        }
        return output.toString();
    }
}
