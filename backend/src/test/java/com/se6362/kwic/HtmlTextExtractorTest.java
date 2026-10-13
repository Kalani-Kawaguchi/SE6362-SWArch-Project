package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.se6362.kwic.index.HtmlTextExtractor;

public class HtmlTextExtractorTest
{
    @Test
    void keepsInlineDescendantText()
    {
        String html = "<p>The <a href='x'>keyword</a> is <strong>important</strong>.</p>";

        List<String> lines = new HtmlTextExtractor().extractLines(html);

        assertTrue(lines.contains("The keyword is important."),
                "inline <a>/<strong> text must be preserved, got: " + lines);
    }

    @Test
    void doesNotDuplicateNestedBlocks()
    {
        // A <p> nested inside an <li> must not produce two separate lines.
        String html = "<ul><li>outer text <p>inner text</p></li></ul>";

        List<String> lines = new HtmlTextExtractor().extractLines(html);

        assertEquals(1, lines.size(), "nested block duplicated, got: " + lines);
        assertTrue(lines.get(0).contains("inner text"));
    }

    @Test
    void extractsBlockTextAndDropsScriptAndStyle()
    {
        String html = "<html><head><title>My Page</title>"
                + "<style>.x{color:red}</style></head><body>"
                + "<h1>Hello World</h1>"
                + "<p>The quick brown fox</p>"
                + "<script>var a = 1;</script>"
                + "<ul><li>first item</li><li>second item</li></ul>"
                + "</body></html>";

        List<String> lines = new HtmlTextExtractor().extractLines(html);

        assertTrue(lines.contains("My Page"));
        assertTrue(lines.contains("Hello World"));
        assertTrue(lines.contains("The quick brown fox"));
        assertTrue(lines.contains("first item"));
        assertFalse(lines.stream().anyMatch(l -> l.contains("var a")));
        assertFalse(lines.stream().anyMatch(l -> l.contains("color:red")));
    }

    @Test
    void skipsVeryLongLinesAndBlanks()
    {
        StringBuilder big = new StringBuilder("<p>");
        for (int i = 0; i < 200; i++)
        {
            big.append("word ");
        }
        big.append("</p><p>   </p><p>keep me</p>");

        List<String> lines = new HtmlTextExtractor().extractLines(big.toString());

        assertTrue(lines.contains("keep me"));
        assertFalse(lines.stream().anyMatch(l -> l.split("\\s+").length > 50));
    }
}
