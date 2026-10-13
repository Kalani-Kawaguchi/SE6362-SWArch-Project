package com.se6362.kwic.index;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlTextExtractor
{
    private static final int MAX_WORDS_PER_LINE = 50;
    private static final int MAX_LINES_PER_DOC = 500;
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
            String text = element.ownText().trim();
            if (text.isEmpty())
            {
                text = element.text().trim();
            }
            if (text.isEmpty())
            {
                continue;
            }
            if (text.split("\\s+").length > MAX_WORDS_PER_LINE)
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
}
