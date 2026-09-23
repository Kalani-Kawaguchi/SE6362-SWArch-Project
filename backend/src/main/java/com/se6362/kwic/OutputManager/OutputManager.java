package com.se6362.kwic.OutputManager;

import com.se6362.kwic.LineList.AlphabetizedLineList;

public class OutputManager
{
    private AlphabetizedLineList outputBuffer = new AlphabetizedLineList();

    public synchronized void writeToBuffer(AlphabetizedLineList newList)
    {
        outputBuffer = new AlphabetizedLineList(outputBuffer, newList);
    }

    public void outputBuffer()
    {
        outputBuffer.printLines();
    }
}
