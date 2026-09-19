package com.se6362.kwic.WordList;

import java.util.ArrayList;

public abstract class WordList {
    private ArrayList<ArrayList<String>> lines;

    public String getWord(int lineNumber, int wordNumber) {
        try {
            return lines.get(lineNumber).get(wordNumber);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    public abstract void setWord(int lineNumber, int wordNumber, String word);
    /*
     * {
     * try
     * {
     * if (word.isBlank())
     * {
     * throw new IllegalArgumentException("Blank string not allowed.");
     * }
     * if (lineNumber == lines.size() && wordNumber == 0)
     * {
     * lines.add(new ArrayList<>());
     * lines.get(lineNumber).add(word);
     * }
     * }
     * catch(IndexOutOfBoundsException e)
     * {
     * 
     * }
     * catch(IllegalArgumentException e)
     * {
     * 
     * }
     * 
     * }
     */

    public int getWordCount(int lineNumber) {
        try {
            return lines.get(lineNumber).size();
        } catch (IndexOutOfBoundsException e) {
            return 0;
        }
    }

    public abstract void readLines(WordList wordList);

}
