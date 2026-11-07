package com.se6362.core;

public class ShiftedDescriptor extends Descriptor
{
    private int shiftAmount;

    public ShiftedDescriptor(Descriptor descriptor, int shiftAmount)
    {
        super(descriptor);
        this.shiftAmount = shiftAmount;
    }

    @Override
    public String getWord(int index)
    {
        return super.getWord((index + shiftAmount) % getLength());
    }

    public int getShiftAmount()
    {
        return shiftAmount;
    }
}
