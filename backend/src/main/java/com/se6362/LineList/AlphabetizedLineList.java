package com.se6362.LineList;

import java.util.ArrayList;

public class AlphabetizedLineList extends LineList
{
    public void processLines(LineList lineList)
    {
        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            int nextLine = super.getLineCount();
            for (int wordNo = 0; wordNo < lineList.getWordCount(lineNo); wordNo++)
            {
                super.setWord(nextLine, wordNo, lineList.getWord(lineNo, wordNo));
            }
        }
        quickSortLines(0, super.getLineCount() - 1);
    }

    private boolean isGreaterThan(int leftLineNo, int rightLineNo)
    {
        int wordNo = 0;
        while (wordNo < super.getWordCount(leftLineNo) && wordNo < super.getWordCount(rightLineNo))
        {
            String leftWord = super.getWord(leftLineNo, wordNo);
            String rightWord = super.getWord(rightLineNo, wordNo);
            int comp = leftWord.compareToIgnoreCase(rightWord);
            if (comp > 0)
            {
                return true;
            }
            if (comp < 0)
            {
                return false;
            }
            wordNo++;
        }
        return wordNo < super.getWordCount(leftLineNo);
    }

    private void swapLines(int lineNo1, int lineNo2)
    {
        ArrayList<String> temp = new ArrayList<>();
        for (int wordNo = 0; wordNo < getWordCount(lineNo1); wordNo++)
        {
            temp.add(getWord(lineNo1, wordNo));
        }
        deleteLine(lineNo1);
        for (int wordNo = 0; wordNo < getWordCount(lineNo2); wordNo++)
        {
            setWord(lineNo1, wordNo, getWord(lineNo2, wordNo));
        }
        deleteLine(lineNo2);
        for (int wordNo = 0; wordNo < temp.size(); wordNo++)
        {
            setWord(lineNo2, wordNo, temp.get(wordNo));
        }
    }

    private int partitionLines(int low, int high)
    {
        int i = low;
        for (int j = low; j < high; j++)
        {
            if (!this.isGreaterThan(j, high))
            {
                swapLines(i, j);
                i++;
            }
        }
        swapLines(i, high);
        return i;
    }

    private void quickSortLines(int low, int high)
    {
        if (low < high)
        {
            int pivot = partitionLines(low, high);
            quickSortLines(low, pivot - 1);
            quickSortLines(pivot + 1, high);
        }
    }
}
