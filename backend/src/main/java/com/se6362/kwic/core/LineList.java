package com.se6362.kwic.core;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import com.se6362.kwic.core.Line;

public abstract class LineList
{
    protected ArrayList<Line> lines = new ArrayList<>();

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

    public String getWord(int lineNumber, int wordNumber)
    {
        return lines.get(lineNumber).getWord(wordNumber);
    }

    protected void setWord(int lineNumber, int wordNumber, String word)
    {
        try
        {
            if (word == null || word.isBlank())
            {
                throw new IllegalArgumentException("Blank string not allowed.");
            }
            if (lineNumber == lines.size() && wordNumber == 0)
            {
                lines.add(new Line());
            }
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

    protected void appendLine(LineList lineList, int lineNo)
    {
        int nextLine = this.getLineCount();
        for (int wordNo = 0; wordNo < lineList.getWordCount(lineNo); wordNo++)
        {
            this.setWord(nextLine, wordNo, lineList.getWord(lineNo, wordNo));
        }
        this.setURI(nextLine, lineList.getURI(lineNo));
    }

        public String getLineText(int lineNumber)
    {
        return String.join(" ", lines.get(lineNumber).getWords());
    }

    public List<Integer> findLinesStartingWith(String keyword)
    {
        List<Integer> matches = new ArrayList<>();

        int low = 0;
        int high = lines.size();
        while (low < high)
        {
            int mid = (low + high) / 2;
            if (lines.get(mid).getWord(0).compareToIgnoreCase(keyword) < 0)
            {
                low = mid + 1;
            }
            else
            {
                high = mid;
            }
        }

        for (int lineNo = low; lineNo < lines.size(); lineNo++)
        {
            if (!lines.get(lineNo).getWord(0).equalsIgnoreCase(keyword))
            {
                break;
            }
            matches.add(lineNo);
        }

        return matches;
    }
    
    public int getWordCount(int lineNumber)
    {
        return lines.get(lineNumber).getWordCount();
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
