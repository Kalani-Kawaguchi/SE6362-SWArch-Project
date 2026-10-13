package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.se6362.kwic.core.CircularShift;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.core.Rotation;

public class RotationExtractionTest
{
    @Test
    void rotationsCarryFirstWordTextAndUri()
    {
        LineStorage storage = new LineStorage();
        storage.storeLine("quick brown fox", URI.create("http://a"));
        CircularShift shifts = new CircularShift();
        shifts.circularShiftLines(storage);

        List<Rotation> rotations = shifts.getRotations();

        assertEquals(3, rotations.size());
        assertEquals("quick", rotations.get(0).firstWord());
        assertEquals("quick brown fox", rotations.get(0).text());
        assertEquals("http://a", rotations.get(0).uri());
        assertEquals("brown", rotations.get(1).firstWord());
        assertEquals("brown fox quick", rotations.get(1).text());
    }
}
