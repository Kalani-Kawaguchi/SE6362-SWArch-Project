package com.se6362.kwic.LineList;

import java.util.ArrayList;

public class AlphabetizedLineList extends LineList
{
    public AlphabetizedLineList(LineList lineList)
    {
        for (int lineNo = 0; lineNo < lineList.getLineCount(); lineNo++)
        {
            this.appendLine(lineList, lineNo);
        }
        quickSortLines(0, super.getLineCount() - 1);
    }

    public AlphabetizedLineList(AlphabetizedLineList oldList, AlphabetizedLineList newList)
    {
        this.mergeLines(oldList, newList);
    }

    public AlphabetizedLineList()
    {
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
            if (!LineList.isGreaterThan(this, this, j, high))
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

    private void mergeLines(AlphabetizedLineList oldList, AlphabetizedLineList newList)
    {
        int oldListPointer = 0;
        int newListPointer = 0;
        while (oldListPointer + newListPointer < oldList.getLineCount() + newList.getLineCount())
        {
            if (LineList.isGreaterThan(oldList, newList, oldListPointer, newListPointer))
            {
                this.appendLine(newList, newListPointer);
                newListPointer++;
            }
            else
            {
                this.appendLine(oldList, oldListPointer);
                oldListPointer++;
            }
        }
    }
}
