package com.se6362.kwic.core;

public class MergedLines extends LineList {
    public void merge(LineList oldList, LineList newList) {
        this.clear();
        int oldListPointer = 0;
        int newListPointer = 0;
        while (oldListPointer < oldList.getLineCount() && newListPointer < newList.getLineCount()) {
            if (compareLines(oldList.lines.get(oldListPointer), newList.lines.get(newListPointer)) > 0) {
                appendLineReference(newList, newListPointer);
                newListPointer++;
            } else {
                appendLineReference(oldList, oldListPointer);
                oldListPointer++;
            }
        }

        while (oldListPointer < oldList.getLineCount()) {
            appendLineReference(oldList, oldListPointer);
            oldListPointer++;
        }

        while (newListPointer < newList.getLineCount()) {
            appendLineReference(newList, newListPointer);
            newListPointer++;
        }
    }

    // compareLines() in LineList should be sufficient
    // private static boolean isGreaterThan(LineList leftList, LineList rightList,
    // int leftLineNo, int rightLineNo)
    // {
    // // a line that does not exist is always assumed to be greater than one that
    // does
    // // exist
    // // this helps with implementation of merge-sort
    // if (leftLineNo >= leftList.getLineCount())
    // {
    // return true;
    // }
    // if (rightLineNo >= rightList.getLineCount())
    // {
    // return false;
    // }
    // int wordNo = 0;
    // while (wordNo < leftList.getWordCount(leftLineNo) && wordNo <
    // rightList.getWordCount(rightLineNo))
    // {
    // String leftWord = leftList.getWord(leftLineNo, wordNo);
    // String rightWord = rightList.getWord(rightLineNo, wordNo);
    // int comp = leftWord.compareToIgnoreCase(rightWord);
    // if (comp > 0)
    // {
    // return true;
    // }
    // if (comp < 0)
    // {
    // return false;
    // }
    // wordNo++;
    // }
    // return wordNo < leftList.getWordCount(leftLineNo);
    // }
}
