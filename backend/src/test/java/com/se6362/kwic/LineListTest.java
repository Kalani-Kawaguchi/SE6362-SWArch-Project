/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: LineListTest.java
    Description: Tests LineList class and all subclasses individually
 */

package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.core.PageList;
import com.se6362.core.WebPage;

@ExtendWith(MockitoExtension.class)
public class LineListTest
{

    @Test
    void testStorageLineList()
    {
        PageList lineList = new PageList();
        lineList.appendPage(new WebPage("", "", "I am testing this method"));
        lineList.appendPage(new WebPage("", "", "Here is a second line to look at"));

        // Assert Junit style: (Expected, Actual)
        assertEquals(lineList.getPageCount(), 2);
        assertEquals(lineList.getPage(0).getDescriptor().getLength(), 5);
        assertEquals(lineList.getPage(1).getDescriptor().getLength(), 8);

        assertEquals(lineList.getPage(0).getDescriptor().getWord(0), "I");
        assertEquals(lineList.getPage(0).getDescriptor().getWord(1), "am");
        assertEquals(lineList.getPage(0).getDescriptor().getWord(2), "testing");
        assertEquals(lineList.getPage(0).getDescriptor().getWord(3), "this");
        assertEquals(lineList.getPage(0).getDescriptor().getWord(4), "method");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(0), "Here");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(1), "is");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(2), "a");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(3), "second");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(4), "line");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(5), "to");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(6), "look");
        assertEquals(lineList.getPage(1).getDescriptor().getWord(7), "at");

    }

    @Test
    void testCircShiftedLineList()
    {
        PageList lineList = new PageList();
        lineList.appendPage(new WebPage("title", "http://example1.com", "a b c d e"));
        lineList.appendPage(new WebPage("title", "http://example2.com", "aa bb cc dd ee ff"));
        CircularShift circularShifts = new CircularShift();
        circularShifts.circularShiftLines(lineList);

        assertEquals(circularShifts.getPageCount(), 11);

        // Line 0: "a b c d e"
        assertEquals("a", circularShifts.getPage(0).getDescriptor().getWord(0));
        assertEquals("b", circularShifts.getPage(0).getDescriptor().getWord(1));
        assertEquals("c", circularShifts.getPage(0).getDescriptor().getWord(2));
        assertEquals("d", circularShifts.getPage(0).getDescriptor().getWord(3));
        assertEquals("e", circularShifts.getPage(0).getDescriptor().getWord(4));
        assertEquals("http://example1.com", circularShifts.getPage(0).getUrl());

        // Line 1: "b c d e a"
        assertEquals("b", circularShifts.getPage(1).getDescriptor().getWord(0));
        assertEquals("c", circularShifts.getPage(1).getDescriptor().getWord(1));
        assertEquals("d", circularShifts.getPage(1).getDescriptor().getWord(2));
        assertEquals("e", circularShifts.getPage(1).getDescriptor().getWord(3));
        assertEquals("a", circularShifts.getPage(1).getDescriptor().getWord(4));
        assertEquals("http://example1.com", circularShifts.getPage(1).getUrl());

        // Line 2: "c d e a b"
        assertEquals("c", circularShifts.getPage(2).getDescriptor().getWord(0));
        assertEquals("d", circularShifts.getPage(2).getDescriptor().getWord(1));
        assertEquals("e", circularShifts.getPage(2).getDescriptor().getWord(2));
        assertEquals("a", circularShifts.getPage(2).getDescriptor().getWord(3));
        assertEquals("b", circularShifts.getPage(2).getDescriptor().getWord(4));
        assertEquals("http://example1.com", circularShifts.getPage(2).getUrl());

        // Line 3: "d e a b c"
        assertEquals("d", circularShifts.getPage(3).getDescriptor().getWord(0));
        assertEquals("e", circularShifts.getPage(3).getDescriptor().getWord(1));
        assertEquals("a", circularShifts.getPage(3).getDescriptor().getWord(2));
        assertEquals("b", circularShifts.getPage(3).getDescriptor().getWord(3));
        assertEquals("c", circularShifts.getPage(3).getDescriptor().getWord(4));
        assertEquals("http://example1.com", circularShifts.getPage(3).getUrl());

        // Line 4: "e a b c d"
        assertEquals("e", circularShifts.getPage(4).getDescriptor().getWord(0));
        assertEquals("a", circularShifts.getPage(4).getDescriptor().getWord(1));
        assertEquals("b", circularShifts.getPage(4).getDescriptor().getWord(2));
        assertEquals("c", circularShifts.getPage(4).getDescriptor().getWord(3));
        assertEquals("d", circularShifts.getPage(4).getDescriptor().getWord(4));
        assertEquals("http://example1.com", circularShifts.getPage(4).getUrl());

        // Line 5: "aa bb cc dd e ff"
        assertEquals("aa", circularShifts.getPage(5).getDescriptor().getWord(0));
        assertEquals("bb", circularShifts.getPage(5).getDescriptor().getWord(1));
        assertEquals("cc", circularShifts.getPage(5).getDescriptor().getWord(2));
        assertEquals("dd", circularShifts.getPage(5).getDescriptor().getWord(3));
        assertEquals("ee", circularShifts.getPage(5).getDescriptor().getWord(4));
        assertEquals("ff", circularShifts.getPage(5).getDescriptor().getWord(5));
        assertEquals("http://example2.com", circularShifts.getPage(5).getUrl());

        // Line 6: "bb cc dd e ff aa"
        assertEquals("bb", circularShifts.getPage(6).getDescriptor().getWord(0));
        assertEquals("cc", circularShifts.getPage(6).getDescriptor().getWord(1));
        assertEquals("dd", circularShifts.getPage(6).getDescriptor().getWord(2));
        assertEquals("ee", circularShifts.getPage(6).getDescriptor().getWord(3));
        assertEquals("ff", circularShifts.getPage(6).getDescriptor().getWord(4));
        assertEquals("aa", circularShifts.getPage(6).getDescriptor().getWord(5));
        assertEquals("http://example2.com", circularShifts.getPage(6).getUrl());

        // Line 7: "cc dd e ff aa bb"
        assertEquals("cc", circularShifts.getPage(7).getDescriptor().getWord(0));
        assertEquals("dd", circularShifts.getPage(7).getDescriptor().getWord(1));
        assertEquals("ee", circularShifts.getPage(7).getDescriptor().getWord(2));
        assertEquals("ff", circularShifts.getPage(7).getDescriptor().getWord(3));
        assertEquals("aa", circularShifts.getPage(7).getDescriptor().getWord(4));
        assertEquals("bb", circularShifts.getPage(7).getDescriptor().getWord(5));
        assertEquals("http://example2.com", circularShifts.getPage(7).getUrl());

        // Line 8: "dd e ff aa bb cc"
        assertEquals("dd", circularShifts.getPage(8).getDescriptor().getWord(0));
        assertEquals("ee", circularShifts.getPage(8).getDescriptor().getWord(1));
        assertEquals("ff", circularShifts.getPage(8).getDescriptor().getWord(2));
        assertEquals("aa", circularShifts.getPage(8).getDescriptor().getWord(3));
        assertEquals("bb", circularShifts.getPage(8).getDescriptor().getWord(4));
        assertEquals("cc", circularShifts.getPage(8).getDescriptor().getWord(5));
        assertEquals("http://example2.com", circularShifts.getPage(8).getUrl());

        // Line 9: "e ff aa bb cc dd"
        assertEquals("ee", circularShifts.getPage(9).getDescriptor().getWord(0));
        assertEquals("ff", circularShifts.getPage(9).getDescriptor().getWord(1));
        assertEquals("aa", circularShifts.getPage(9).getDescriptor().getWord(2));
        assertEquals("bb", circularShifts.getPage(9).getDescriptor().getWord(3));
        assertEquals("cc", circularShifts.getPage(9).getDescriptor().getWord(4));
        assertEquals("dd", circularShifts.getPage(9).getDescriptor().getWord(5));
        assertEquals("http://example2.com", circularShifts.getPage(9).getUrl());

        // Line 10: "ff aa bb cc dd e"
        assertEquals("ff", circularShifts.getPage(10).getDescriptor().getWord(0));
        assertEquals("aa", circularShifts.getPage(10).getDescriptor().getWord(1));
        assertEquals("bb", circularShifts.getPage(10).getDescriptor().getWord(2));
        assertEquals("cc", circularShifts.getPage(10).getDescriptor().getWord(3));
        assertEquals("dd", circularShifts.getPage(10).getDescriptor().getWord(4));
        assertEquals("ee", circularShifts.getPage(10).getDescriptor().getWord(5));
        assertEquals("http://example2.com", circularShifts.getPage(10).getUrl());
    }

    @Test
    void testAlphabetizedLineList()
    {
        PageList lineList = new PageList();
        lineList.appendPage(new WebPage("title", "http://example.com", "aa aa aa"));
        lineList.appendPage(new WebPage("title", "http://example.com", "bb aa cc"));
        lineList.appendPage(new WebPage("title", "http://example.com", "cc cc cc"));
        lineList.appendPage(new WebPage("title", "http://example.com", "aa"));
        lineList.appendPage(new WebPage("title", "http://example.com", "aa bb"));
        lineList.appendPage(new WebPage("title", "http://example.com", "aa bb cc"));
        lineList.appendPage(new WebPage("title", "http://example.com", "aa bb cc"));
        lineList.appendPage(new WebPage("title", "http://example.com", "aa bc cd"));
        lineList.appendPage(new WebPage("title", "http://example.com", "aa cc bb"));
        lineList.appendPage(new WebPage("title", "http://example.com", "ab bb cc"));
        lineList.appendPage(new WebPage("title", "http://example.com", "bb cc aa"));
        lineList.appendPage(new WebPage("title", "http://example.com", "bb bb bb"));
        lineList.appendPage(new WebPage("title", "http://example.com", "cc aa bb"));
        lineList.appendPage(new WebPage("title", "http://example.com", "cc bb aa"));

        Alphabetizer alphabetizedLineList = new Alphabetizer();
        alphabetizedLineList.alphabetizeLines(lineList);

        // 0: "aa"
        assertEquals("aa", alphabetizedLineList.getPage(0).getDescriptor().getWord(0));

        // 1: "aa aa aa"
        assertEquals("aa", alphabetizedLineList.getPage(1).getDescriptor().getWord(0));
        assertEquals("aa", alphabetizedLineList.getPage(1).getDescriptor().getWord(1));
        assertEquals("aa", alphabetizedLineList.getPage(1).getDescriptor().getWord(2));

        // 2: "aa bb"
        assertEquals("aa", alphabetizedLineList.getPage(2).getDescriptor().getWord(0));
        assertEquals("bb", alphabetizedLineList.getPage(2).getDescriptor().getWord(1));

        // 3: "aa bb cc"
        assertEquals("aa", alphabetizedLineList.getPage(3).getDescriptor().getWord(0));
        assertEquals("bb", alphabetizedLineList.getPage(3).getDescriptor().getWord(1));
        assertEquals("cc", alphabetizedLineList.getPage(3).getDescriptor().getWord(2));

        // 4: "aa bc cd"
        assertEquals("aa", alphabetizedLineList.getPage(4).getDescriptor().getWord(0));
        assertEquals("bc", alphabetizedLineList.getPage(4).getDescriptor().getWord(1));
        assertEquals("cd", alphabetizedLineList.getPage(4).getDescriptor().getWord(2));

        // 5: "aa cc bb"
        assertEquals("aa", alphabetizedLineList.getPage(5).getDescriptor().getWord(0));
        assertEquals("cc", alphabetizedLineList.getPage(5).getDescriptor().getWord(1));
        assertEquals("bb", alphabetizedLineList.getPage(5).getDescriptor().getWord(2));

        // 6: "ab bb cc"
        assertEquals("ab", alphabetizedLineList.getPage(6).getDescriptor().getWord(0));
        assertEquals("bb", alphabetizedLineList.getPage(6).getDescriptor().getWord(1));
        assertEquals("cc", alphabetizedLineList.getPage(6).getDescriptor().getWord(2));

        // 7: "bb aa cc"
        assertEquals("bb", alphabetizedLineList.getPage(7).getDescriptor().getWord(0));
        assertEquals("aa", alphabetizedLineList.getPage(7).getDescriptor().getWord(1));
        assertEquals("cc", alphabetizedLineList.getPage(7).getDescriptor().getWord(2));

        // 8: "bb bb bb"
        assertEquals("bb", alphabetizedLineList.getPage(8).getDescriptor().getWord(0));
        assertEquals("bb", alphabetizedLineList.getPage(8).getDescriptor().getWord(1));
        assertEquals("bb", alphabetizedLineList.getPage(8).getDescriptor().getWord(2));

        // 9: "bb cc aa"
        assertEquals("bb", alphabetizedLineList.getPage(9).getDescriptor().getWord(0));
        assertEquals("cc", alphabetizedLineList.getPage(9).getDescriptor().getWord(1));
        assertEquals("aa", alphabetizedLineList.getPage(9).getDescriptor().getWord(2));

        // 10: "cc aa bb"
        assertEquals("cc", alphabetizedLineList.getPage(10).getDescriptor().getWord(0));
        assertEquals("aa", alphabetizedLineList.getPage(10).getDescriptor().getWord(1));
        assertEquals("bb", alphabetizedLineList.getPage(10).getDescriptor().getWord(2));

        // 11: "cc bb aa"
        assertEquals("cc", alphabetizedLineList.getPage(11).getDescriptor().getWord(0));
        assertEquals("bb", alphabetizedLineList.getPage(11).getDescriptor().getWord(1));
        assertEquals("aa", alphabetizedLineList.getPage(11).getDescriptor().getWord(2));

        // 12: "cc cc cc"
        assertEquals("cc", alphabetizedLineList.getPage(12).getDescriptor().getWord(0));
        assertEquals("cc", alphabetizedLineList.getPage(12).getDescriptor().getWord(1));
        assertEquals("cc", alphabetizedLineList.getPage(12).getDescriptor().getWord(2));
    }

    @Test
    void testMergedLines()
    {

        PageList lineList1 = new PageList();
        PageList lineList2 = new PageList();
        lineList1.appendPage(new WebPage("title", "http://example.com", "aa aa aa"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "bb aa cc"));
        lineList1.appendPage(new WebPage("title", "http://example.com", "cc cc cc"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "aa"));
        lineList1.appendPage(new WebPage("title", "http://example.com", "aa bb"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "aa bb cc"));
        lineList1.appendPage(new WebPage("title", "http://example.com", "aa bb cc"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "aa bc cd"));
        lineList1.appendPage(new WebPage("title", "http://example.com", "aa cc bb"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "ab bb cc"));
        lineList1.appendPage(new WebPage("title", "http://example.com", "bb cc aa"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "bb bb bb"));
        lineList1.appendPage(new WebPage("title", "http://example.com", "cc aa bb"));
        lineList2.appendPage(new WebPage("title", "http://example.com", "cc bb aa"));

        Alphabetizer alpha1 = new Alphabetizer();
        alpha1.alphabetizeLines(lineList1);
        Alphabetizer alpha2 = new Alphabetizer();
        alpha2.alphabetizeLines(lineList2);
        MergedLines merge = new MergedLines();
        merge.mergeLines(alpha1, alpha2);

        // 0: "aa"
        assertEquals("aa", merge.getPage(0).getDescriptor().getWord(0));

        // 1: "aa aa aa"
        assertEquals("aa", merge.getPage(1).getDescriptor().getWord(0));
        assertEquals("aa", merge.getPage(1).getDescriptor().getWord(1));
        assertEquals("aa", merge.getPage(1).getDescriptor().getWord(2));

        // 2: "aa bb"
        assertEquals("aa", merge.getPage(2).getDescriptor().getWord(0));
        assertEquals("bb", merge.getPage(2).getDescriptor().getWord(1));

        // 3: "aa bb cc"
        assertEquals("aa", merge.getPage(3).getDescriptor().getWord(0));
        assertEquals("bb", merge.getPage(3).getDescriptor().getWord(1));
        assertEquals("cc", merge.getPage(3).getDescriptor().getWord(2));

        // 4: "aa bc cd"
        assertEquals("aa", merge.getPage(4).getDescriptor().getWord(0));
        assertEquals("bc", merge.getPage(4).getDescriptor().getWord(1));
        assertEquals("cd", merge.getPage(4).getDescriptor().getWord(2));

        // 5: "aa cc bb"
        assertEquals("aa", merge.getPage(5).getDescriptor().getWord(0));
        assertEquals("cc", merge.getPage(5).getDescriptor().getWord(1));
        assertEquals("bb", merge.getPage(5).getDescriptor().getWord(2));

        // 6: "ab bb cc"
        assertEquals("ab", merge.getPage(6).getDescriptor().getWord(0));
        assertEquals("bb", merge.getPage(6).getDescriptor().getWord(1));
        assertEquals("cc", merge.getPage(6).getDescriptor().getWord(2));

        // 7: "bb aa cc"
        assertEquals("bb", merge.getPage(7).getDescriptor().getWord(0));
        assertEquals("aa", merge.getPage(7).getDescriptor().getWord(1));
        assertEquals("cc", merge.getPage(7).getDescriptor().getWord(2));

        // 8: "bb bb bb"
        assertEquals("bb", merge.getPage(8).getDescriptor().getWord(0));
        assertEquals("bb", merge.getPage(8).getDescriptor().getWord(1));
        assertEquals("bb", merge.getPage(8).getDescriptor().getWord(2));

        // 9: "bb cc aa"
        assertEquals("bb", merge.getPage(9).getDescriptor().getWord(0));
        assertEquals("cc", merge.getPage(9).getDescriptor().getWord(1));
        assertEquals("aa", merge.getPage(9).getDescriptor().getWord(2));

        // 10: "cc aa bb"
        assertEquals("cc", merge.getPage(10).getDescriptor().getWord(0));
        assertEquals("aa", merge.getPage(10).getDescriptor().getWord(1));
        assertEquals("bb", merge.getPage(10).getDescriptor().getWord(2));

        // 11: "cc bb aa"
        assertEquals("cc", merge.getPage(11).getDescriptor().getWord(0));
        assertEquals("bb", merge.getPage(11).getDescriptor().getWord(1));
        assertEquals("aa", merge.getPage(11).getDescriptor().getWord(2));

        // 12: "cc cc cc"
        assertEquals("cc", merge.getPage(12).getDescriptor().getWord(0));
        assertEquals("cc", merge.getPage(12).getDescriptor().getWord(1));
        assertEquals("cc", merge.getPage(12).getDescriptor().getWord(2));
    }
}
