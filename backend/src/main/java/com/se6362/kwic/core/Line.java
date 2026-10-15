/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: Line.java
    Description: Simple data structure containing an array of words and a uri element
 */

package com.se6362.kwic.core;

import java.net.URI;
import java.util.ArrayList;

public class Line
{
    private ArrayList<String> words = new ArrayList<>();
    private URI uri = null;

    // Gets the word located at wordNumber
    public String getWord(int wordNumber)
    {
        return words.get(wordNumber);
    }

    // Sets the word located at wordNumber to a new word
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

    // Gets the URI associated with this line of text
    public URI getURI()
    {
        return uri;
    }

    // Sets the URI associated with this line of text
    public void setURI(URI uri)
    {
        this.uri = uri;
    }

    // Gets the number of words currently stored in this line
    public int getWordCount()
    {
        return words.size();
    }

}
