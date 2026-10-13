package com.se6362.kwic.index;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class HtmlTextExtractor
{
    private static final int MAX_WORDS_PER_LINE = 50;
    private static final int MAX_LINES_PER_DOC = 500;
    // Keep each line (and therefore every rotation of it) within the DB column
    // bounds: line_text VARCHAR(1024) and first_word VARCHAR(255).
    private static final int MAX_CHARS_PER_LINE = 1000;
    private static final int MAX_CHARS_PER_WORD = 200;
    private static final String BLOCK_SELECTOR = "title, h1, h2, h3, h4, h5, h6, p, li";

    public List<String> extractLines(String html)
    {
        List<String> lines = new ArrayList<>();
        if (html == null || html.isBlank())
        {
            return lines;
        }

        Document document = Jsoup.parse(html);
        document.select("script, style, noscript").remove();

        for (Element element : document.select(BLOCK_SELECTOR))
        {
            // Skip a block nested inside another selected block; its text is
            // already captured by the ancestor, so we avoid duplicate lines.
            if (hasSelectedAncestor(element))
            {
                continue;
            }

            // Full descendant text (including inline <a>/<strong>/etc.), with
            // whitespace normalized by Jsoup.
            String text = element.text().trim();
            if (text.isEmpty())
            {
                continue;
            }

            String[] words = text.split("\\s+");
            if (words.length > MAX_WORDS_PER_LINE || text.length() > MAX_CHARS_PER_LINE)
            {
                continue;
            }
            if (hasOverlongWord(words))
            {
                continue;
            }

            lines.add(text);
            if (lines.size() >= MAX_LINES_PER_DOC)
            {
                break;
            }
        }
        return lines;
    }

    private boolean hasSelectedAncestor(Element element)
    {
        for (Element parent : element.parents())
        {
            if (parent.is(BLOCK_SELECTOR))
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasOverlongWord(String[] words)
    {
        for (String word : words)
        {
            if (word.length() > MAX_CHARS_PER_WORD)
            {
                return true;
            }
        }
        return false;
    }
}
