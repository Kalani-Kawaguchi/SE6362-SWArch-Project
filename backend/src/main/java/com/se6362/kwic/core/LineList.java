package com.se6362.kwic.core;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class LineList
{
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    protected ArrayList<ArrayList<String>> lines = new ArrayList<>();

    public String getWord(int lineNumber, int wordNumber)
    {
        return lines.get(lineNumber).get(wordNumber);
    }

    protected void setWord(int lineNumber, int wordNumber, String word)
    {
        try
        {
            if (word == null || word.isBlank())
            {
                throw new IllegalArgumentException("Blank string not allowed.");
            }
            if (lineNumber == lines.size() && wordNumber == 0) // adding a new line
            {
                lines.add(new ArrayList<>());
            }
            ArrayList<String> line = lines.get(lineNumber);
            if (wordNumber == line.size()) // appending a new word to end of a line
            {
                line.add(word);
            }
            else // replacing a word in an existing line
            {
                line.set(wordNumber, word);
            }
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

    protected void appendLine(LineList lineList, int lineNo)
    {
        int nextLine = this.getLineCount();
        for (int wordNo = 0; wordNo < lineList.getWordCount(lineNo); wordNo++)
        {
            this.setWord(nextLine, wordNo, lineList.getWord(lineNo, wordNo));
        }
    }

    public int getWordCount(int lineNumber)
    {
        return lines.get(lineNumber).size();
    }

    public int getLineCount()
    {
        return lines.size();
    }

    public void clear()
    {
        lines.clear();
    }

    // returns 0 if they are the same, negative if leftLine is less than rightLine,
    // positive if leftLine is greater
    protected static int compareLines(List<String> leftLine, List<String> rightLine)
    {
        int wordCount = Math.min(leftLine.size(), rightLine.size());

        for (int wordNumber = 0; wordNumber < wordCount; wordNumber++)
        {
            int comparison = leftLine.get(wordNumber).compareToIgnoreCase(rightLine.get(wordNumber));
            if (comparison != 0)
            {
                return comparison;
            }
        }
        return Integer.compare(leftLine.size(), rightLine.size());
    }

    public String toString()
    {
        StringBuilder output = new StringBuilder();
        for (ArrayList<String> line : lines)
        {
            for (String word : line)
            {
                output.append(word).append(" ");
            }
            output.append("\n");
        }
        return output.toString();
    }
}
