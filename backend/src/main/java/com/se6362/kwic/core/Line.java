package com.se6362.kwic.core;

import java.net.URI;
import java.util.ArrayList;

public class Line
{
    private ArrayList<String> words = new ArrayList<>();
    private URI uri = null;

    public String getWord(int wordNumber)
    {
        return words.get(wordNumber);
    }

    public void setWord(int wordNumber, String word)
    {
        if (wordNumber == words.size())
        {
            words.add(word);
        }
        else if (wordNumber < words.size())
        {
            words.set(wordNumber, word);
        }
        else
        {
            throw new IllegalArgumentException("Cannot set words beyond (n + 1)-th position.");
        }
    }

    public URI getURI()
    {
        return uri;
    }

    public void setURI(URI uri)
    {
        this.uri = uri;
    }

    public int getWordCount()
    {
        return words.size();
    }

}
