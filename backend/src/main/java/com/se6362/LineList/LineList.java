package com.se6362.LineList;

import java.util.ArrayList;

public class LineList
{
    protected final ArrayList<ArrayList<String>> lines = new ArrayList<>();

    public String getWord(int lineNumber, int wordNumber)
    {
        try
        {
            return lines.get(lineNumber).get(wordNumber);
        }
        catch (IndexOutOfBoundsException e)
        {
            System.err.println("Unable to get word; No word stored at line "
                    + lineNumber + " word " + wordNumber + ".");
            return "";
        }
    }

    public void setWord(int lineNumber, int wordNumber, String word)
    {
        try
        {
            if (word.isBlank())
            {
                throw new IllegalArgumentException("Blank string not allowed.");
            }
            if (lineNumber == lines.size() && wordNumber == 0)
            {
                lines.add(new ArrayList<>());
                lines.get(lineNumber).add(word);
            }
            else if (lineNumber < lines.size() && wordNumber == lines.get(lineNumber).size())
            {
                lines.get(lineNumber).add(word);
            }
            else if (lineNumber < lines.size() && wordNumber < lines.get(lineNumber).size())
            {
                lines.get(lineNumber).set(wordNumber, word);
            }
            else
            {
                throw new IndexOutOfBoundsException("Must set word at or before next empty line or word");
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            System.err.println("Unable to set word at line "
                    + lineNumber + " word " + wordNumber +
                    ". New words can only be appended to the end of a line, " +
                    "and new lines can only be appended to the end of the line list.");
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Unable to set word because word is black." +
                    "Words must contain at least one non-blank character.");
        }
    }

    public int getWordCount(int lineNumber)
    {
        try
        {
            return lines.get(lineNumber).size();
        }
        catch (IndexOutOfBoundsException e)
        {
            System.err.println("Unable to get word count. No words " +
                    "have been entered into line " + lineNumber + ".");
            return 0;
        }
    }

    public int getLineCount()
    {
        return lines.size();
    }

    protected void deleteLine(int lineNumber)
    {
        try
        {
            lines.set(lineNumber, new ArrayList<>());
        }
        catch (IndexOutOfBoundsException e)
        {
            System.err.println("No line at position " + lineNumber + ".");
        }
    }

    public void printLines()
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
        System.out.println(output.toString());
    }
}
