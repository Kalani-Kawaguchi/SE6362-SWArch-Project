package com.se6362.kwic.core;

public class Descriptor
{
    private String[] words;

    public Descriptor(String text)
    {
        words = text.split("\\p{javaWhitespace}+");
    }

    public Descriptor(Descriptor other)
    {
        words = other.getWords();
    }

    protected String[] getWords()
    {
        return words;
    }

    public String getWord(int index)
    {
        return (words[index]).replaceAll("[^\\p{L}\\p{N}\\p{javaWhitespace}]", "").trim();
    }

    public String toString()
    {
        return String.join(" ", words);
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
