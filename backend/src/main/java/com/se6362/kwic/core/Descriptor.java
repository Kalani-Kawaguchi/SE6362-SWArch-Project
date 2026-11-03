package com.se6362.kwic.core;

import java.util.StringJoiner;

public class Descriptor
{
    private String[] words;

    public Descriptor(String text)
    {
        words = text.split("\\p{javaWhitespace}+");
    }

    public Descriptor(Descriptor other)
    {
        words = other.words;
    }

    public String getWord(int index)
    {
        return words[index];
    }

    public String getFilteredWord(int index)
    {
        return this.getWord(index).replaceAll("[^\\p{L}\\p{N}\\p{javaWhitespace}]", "").trim();
    }

    public String toString()
    {
        StringJoiner stringJoiner = new StringJoiner(" ");
        for (int i = 0; i < words.length; i++)
        {
            stringJoiner.add(getWord(i));
        }
        return stringJoiner.toString();
    }

    public String toFilteredString()
    {
        return toString().replaceAll("[^\\p{L}\\p{N}\\p{javaWhitespace}]", "").trim();
    }

    public int getLength()
    {
        return words.length;
    }

}
