package com.se6362.kwic.core;

import java.util.StringJoiner;

public class ShiftedDescriptor extends Descriptor
{
    private int shiftAmount;

    public ShiftedDescriptor(Descriptor descriptor, int shiftAmount)
    {
        super(descriptor);
        this.shiftAmount = shiftAmount;
    }

    @Override
    public String toString()
    {
        String[] words = getWords();
        StringJoiner stringJoiner = new StringJoiner(" ");
        for (int i = 0; i < words.length; i++)
        {
            stringJoiner.add(words[(i + shiftAmount) % words.length]);
        }
        return stringJoiner.toString();
    }

    @Override
    public String getWord(int index)
    {
        return super.getWord((index + shiftAmount) % getLength());
    }
}
