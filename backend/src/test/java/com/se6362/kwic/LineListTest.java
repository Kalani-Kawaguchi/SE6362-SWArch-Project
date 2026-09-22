package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.kwic.LineList.AlphabetizedLineList;
import com.se6362.kwic.LineList.CircShiftedLineList;
import com.se6362.kwic.LineList.StorageLineList;

// 1. Removed unnecessary com.se6362.kwic imports since you are already in this package.
// 2. Cleaned up unused Mockito imports since this specific test uses pure Java logic.

@ExtendWith(MockitoExtension.class)
public class LineListTest
{

    // Instantiating your real object directly (Great choice since it doesn't need
    // external mocks!)

    @Test
    void testStorageLineList()
    {
        StorageLineList lineList = new StorageLineList();
        lineList.addLine("I am testing this method.");
        lineList.addLine("Here is a second line to look at.");

        // Assert Junit style: (Expected, Actual)
        assertEquals(lineList.getLineCount(), 2);
        assertEquals(lineList.getWordCount(0), 5);
        assertEquals(lineList.getWordCount(1), 8);
        assertEquals(lineList.getWord(0, 0), "I");
        assertEquals(lineList.getWord(0, 1), "am");
        assertEquals(lineList.getWord(0, 2), "testing");
        assertEquals(lineList.getWord(0, 3), "this");
        assertEquals(lineList.getWord(0, 4), "method.");
        assertEquals(lineList.getWord(1, 0), "Here");
        assertEquals(lineList.getWord(1, 1), "is");
        assertEquals(lineList.getWord(1, 2), "a");
        assertEquals(lineList.getWord(1, 3), "second");
        assertEquals(lineList.getWord(1, 4), "line");
        assertEquals(lineList.getWord(1, 5), "to");
        assertEquals(lineList.getWord(1, 6), "look");
        assertEquals(lineList.getWord(1, 7), "at.");

    }

    @Test
    void testCircShiftedLineList()
    {
        StorageLineList lineList = new StorageLineList();
        CircShiftedLineList circularShifts = new CircShiftedLineList();
        lineList.addLine("a b c d e");
        lineList.addLine("aa bb cc dd ee ff");

        circularShifts.processLines(lineList);

        // Line 0: "a b c d e"
        assertEquals("a", circularShifts.getWord(0, 0));
        assertEquals("b", circularShifts.getWord(0, 1));
        assertEquals("c", circularShifts.getWord(0, 2));
        assertEquals("d", circularShifts.getWord(0, 3));
        assertEquals("e", circularShifts.getWord(0, 4));

        // Line 1: "b c d e a"
        assertEquals("b", circularShifts.getWord(1, 0));
        assertEquals("c", circularShifts.getWord(1, 1));
        assertEquals("d", circularShifts.getWord(1, 2));
        assertEquals("e", circularShifts.getWord(1, 3));
        assertEquals("a", circularShifts.getWord(1, 4));

        // Line 2: "c d e a b"
        assertEquals("c", circularShifts.getWord(2, 0));
        assertEquals("d", circularShifts.getWord(2, 1));
        assertEquals("e", circularShifts.getWord(2, 2));
        assertEquals("a", circularShifts.getWord(2, 3));
        assertEquals("b", circularShifts.getWord(2, 4));

        // Line 3: "d e a b c"
        assertEquals("d", circularShifts.getWord(3, 0));
        assertEquals("e", circularShifts.getWord(3, 1));
        assertEquals("a", circularShifts.getWord(3, 2));
        assertEquals("b", circularShifts.getWord(3, 3));
        assertEquals("c", circularShifts.getWord(3, 4));

        // Line 4: "e a b c d"
        assertEquals("e", circularShifts.getWord(4, 0));
        assertEquals("a", circularShifts.getWord(4, 1));
        assertEquals("b", circularShifts.getWord(4, 2));
        assertEquals("c", circularShifts.getWord(4, 3));
        assertEquals("d", circularShifts.getWord(4, 4));

        // Line 5: "aa bb cc dd e ff"
        assertEquals("aa", circularShifts.getWord(5, 0));
        assertEquals("bb", circularShifts.getWord(5, 1));
        assertEquals("cc", circularShifts.getWord(5, 2));
        assertEquals("dd", circularShifts.getWord(5, 3));
        assertEquals("ee", circularShifts.getWord(5, 4));
        assertEquals("ff", circularShifts.getWord(5, 5));

        // Line 6: "bb cc dd e ff aa"
        assertEquals("bb", circularShifts.getWord(6, 0));
        assertEquals("cc", circularShifts.getWord(6, 1));
        assertEquals("dd", circularShifts.getWord(6, 2));
        assertEquals("ee", circularShifts.getWord(6, 3));
        assertEquals("ff", circularShifts.getWord(6, 4));
        assertEquals("aa", circularShifts.getWord(6, 5));

        // Line 7: "cc dd e ff aa bb"
        assertEquals("cc", circularShifts.getWord(7, 0));
        assertEquals("dd", circularShifts.getWord(7, 1));
        assertEquals("ee", circularShifts.getWord(7, 2));
        assertEquals("ff", circularShifts.getWord(7, 3));
        assertEquals("aa", circularShifts.getWord(7, 4));
        assertEquals("bb", circularShifts.getWord(7, 5));

        // Line 8: "dd e ff aa bb cc"
        assertEquals("dd", circularShifts.getWord(8, 0));
        assertEquals("ee", circularShifts.getWord(8, 1));
        assertEquals("ff", circularShifts.getWord(8, 2));
        assertEquals("aa", circularShifts.getWord(8, 3));
        assertEquals("bb", circularShifts.getWord(8, 4));
        assertEquals("cc", circularShifts.getWord(8, 5));

        // Line 9: "e ff aa bb cc dd"
        assertEquals("ee", circularShifts.getWord(9, 0));
        assertEquals("ff", circularShifts.getWord(9, 1));
        assertEquals("aa", circularShifts.getWord(9, 2));
        assertEquals("bb", circularShifts.getWord(9, 3));
        assertEquals("cc", circularShifts.getWord(9, 4));
        assertEquals("dd", circularShifts.getWord(9, 5));

        // Line 10: "ff aa bb cc dd e"
        assertEquals("ff", circularShifts.getWord(10, 0));
        assertEquals("aa", circularShifts.getWord(10, 1));
        assertEquals("bb", circularShifts.getWord(10, 2));
        assertEquals("cc", circularShifts.getWord(10, 3));
        assertEquals("dd", circularShifts.getWord(10, 4));
        assertEquals("ee", circularShifts.getWord(10, 5));
    }

    @Test
    void testAlphabetizedLineList()
    {
        StorageLineList lineList = new StorageLineList();
        AlphabetizedLineList alphabetizedLineList = new AlphabetizedLineList();

        lineList.addLine("aa aa aa");
        lineList.addLine("bb bb bb");
        lineList.addLine("cc cc cc");
        lineList.addLine("aa");
        lineList.addLine("aa bb");
        lineList.addLine("aa bb cc");
        lineList.addLine("ab bb cc");
        lineList.addLine("aa bc cd");
        lineList.addLine("aa cc bb");
        lineList.addLine("bb aa cc");
        lineList.addLine("bb cc aa");
        lineList.addLine("cc aa bb");
        lineList.addLine("cc bb aa");

        alphabetizedLineList.processLines(lineList);

        // 0: "aa"
        assertEquals("aa", alphabetizedLineList.getWord(0, 0));

        // 1: "aa aa aa"
        assertEquals("aa", alphabetizedLineList.getWord(1, 0));
        assertEquals("aa", alphabetizedLineList.getWord(1, 1));
        assertEquals("aa", alphabetizedLineList.getWord(1, 2));

        // 2: "aa bb"
        assertEquals("aa", alphabetizedLineList.getWord(2, 0));
        assertEquals("bb", alphabetizedLineList.getWord(2, 1));

        // 3: "aa bb cc"
        assertEquals("aa", alphabetizedLineList.getWord(3, 0));
        assertEquals("bb", alphabetizedLineList.getWord(3, 1));
        assertEquals("cc", alphabetizedLineList.getWord(3, 2));

        // 4: "aa bc cd"
        assertEquals("aa", alphabetizedLineList.getWord(4, 0));
        assertEquals("bc", alphabetizedLineList.getWord(4, 1));
        assertEquals("cd", alphabetizedLineList.getWord(4, 2));

        // 5: "aa cc bb"
        assertEquals("aa", alphabetizedLineList.getWord(5, 0));
        assertEquals("cc", alphabetizedLineList.getWord(5, 1));
        assertEquals("bb", alphabetizedLineList.getWord(5, 2));

        // 6: "ab bb cc"
        assertEquals("ab", alphabetizedLineList.getWord(6, 0));
        assertEquals("bb", alphabetizedLineList.getWord(6, 1));
        assertEquals("cc", alphabetizedLineList.getWord(6, 2));

        // 7: "bb aa cc"
        assertEquals("bb", alphabetizedLineList.getWord(7, 0));
        assertEquals("aa", alphabetizedLineList.getWord(7, 1));
        assertEquals("cc", alphabetizedLineList.getWord(7, 2));

        // 8: "bb bb bb"
        assertEquals("bb", alphabetizedLineList.getWord(8, 0));
        assertEquals("bb", alphabetizedLineList.getWord(8, 1));
        assertEquals("bb", alphabetizedLineList.getWord(8, 2));

        // 9: "bb cc aa"
        assertEquals("bb", alphabetizedLineList.getWord(9, 0));
        assertEquals("cc", alphabetizedLineList.getWord(9, 1));
        assertEquals("aa", alphabetizedLineList.getWord(9, 2));

        // 10: "cc aa bb"
        assertEquals("cc", alphabetizedLineList.getWord(10, 0));
        assertEquals("aa", alphabetizedLineList.getWord(10, 1));
        assertEquals("bb", alphabetizedLineList.getWord(10, 2));

        // 11: "cc bb aa"
        assertEquals("cc", alphabetizedLineList.getWord(11, 0));
        assertEquals("bb", alphabetizedLineList.getWord(11, 1));
        assertEquals("aa", alphabetizedLineList.getWord(11, 2));

        // 12: "cc cc cc"
        assertEquals("cc", alphabetizedLineList.getWord(12, 0));
        assertEquals("cc", alphabetizedLineList.getWord(12, 1));
        assertEquals("cc", alphabetizedLineList.getWord(12, 2));
    }
}
