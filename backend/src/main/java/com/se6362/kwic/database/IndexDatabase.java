/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: IndexDatabase.java
    Description: Module to communicate with KWIC index SQL database
 */

package com.se6362.kwic.database;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.se6362.kwic.core.LineList;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.importer.TsvToJsonl.WebpageRecord;

@Repository
public class IndexDatabase
{
    private static final String SELECT_PAGES = "SELECT Url, Title, Description FROM WEB_PAGE ORDER BY Title";
    private static final RowMapper<WebpageRecord> PAGE_MAPPER = (row, rowNo) -> new WebpageRecord(row.getString("Url"),
            row.getString("Title"), row.getString("Description"));

    private final JdbcTemplate database;

    public IndexDatabase(JdbcTemplate database)
    {
        this.database = database;
    }

    // Database query to get info for all webpages
    @Transactional(readOnly = true)
    public List<WebpageRecord> getPages()
    {
        return List.copyOf(database.query(SELECT_PAGES, PAGE_MAPPER));
    }

    // Database query to get info for webpages with whose descriptions contains at
    // least one of the keywords
    @Transactional(readOnly = true)
    public List<WebpageRecord> search(List<String> keywords)
    {
        if (keywords.isEmpty())
        {
            return List.of();
        }
        String placeholders = String.join(", ", Collections.nCopies(keywords.size(), "?"));
        String sql = """
                SELECT w.Title, w.Url, w.Description
                FROM WEB_PAGE w
                JOIN (
                    SELECT PageTitle, COUNT(DISTINCT Keyword) AS MatchCount
                    FROM INDEX_ENTRY
                    WHERE Keyword IN (%s)
                    GROUP BY PageTitle
                ) matches ON matches.PageTitle = w.Title
                ORDER BY matches.MatchCount DESC, LOWER(w.Title) ASC, w.Title ASC
                """.formatted(placeholders);
        return List.copyOf(database.query(sql, PAGE_MAPPER, keywords.toArray()));
    }

    // Replaces current database index with new index
    @Transactional
    public int replaceIndex(LineList shifts)
    {
        List<WebpageRecord> pages = database.query(SELECT_PAGES + " FOR UPDATE", PAGE_MAPPER);
        List<Object[]> entries = prepareEntries(shifts, pages);
        database.update("DELETE FROM INDEX_ENTRY");
        database.batchUpdate("INSERT INTO INDEX_ENTRY (Keyword, ShiftText, PageTitle) VALUES (?, ?, ?)", entries);
        return entries.size();
    }

    // Format database entries for insertion
    private static List<Object[]> prepareEntries(LineList shifts, List<WebpageRecord> pages)
    {
        if (pages.isEmpty())
        {
            throw new IllegalStateException("No WEB_PAGE entries found.");
        }
        Map<URI, WebpageRecord> pagesByUri = new HashMap<>();
        Map<URI, Map<String, Integer>> remainingWords = new HashMap<>();
        Map<URI, Integer> wordCounts = new HashMap<>();
        for (WebpageRecord page : pages)
        {
            URI uri = URI.create(page.url());
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null)
            {
                throw new IllegalStateException("WEB_PAGE entry must use http or https scheme: " + page.url());
            }
            if (pagesByUri.put(uri, page) != null)
            {
                throw new IllegalStateException("Duplicate WEB_PAGE entry for URL: " + page.url());
            }
            LineStorage source = new LineStorage();
            source.storeLine(page.text());
            if (source.getLineCount() == 0)
            {
                throw new IllegalStateException("WEB_PAGE entry has no text: " + page.title());
            }
            Map<String, Integer> frequencies = new HashMap<>();
            int count = source.getWordCount(0);
            for (int wordNo = 0; wordNo < count; wordNo++)
            {
                frequencies.merge(source.getWord(0, wordNo), 1, Integer::sum);
            }
            remainingWords.put(uri, frequencies);
            wordCounts.put(uri, count);
        }
        List<Object[]> entries = new ArrayList<>();
        for (int lineNo = 0; lineNo < shifts.getLineCount(); lineNo++)
        {
            URI uri = shifts.getURI(lineNo);
            WebpageRecord page = pagesByUri.get(uri);
            if (page == null || shifts.getWordCount(lineNo) != wordCounts.get(uri))
            {
                throw new IllegalStateException("A shift has an unknown URL or incorrect word count.");
            }
            String keyword = shifts.getWord(lineNo, 0);
            if (keyword.codePointCount(0, keyword.length()) > 512)
            {
                throw new IllegalStateException("Keyword exceeds INDEX_ENTRY's 512-character limit.");
            }
            Map<String, Integer> remaining = remainingWords.get(uri);
            int occurrences = remaining.getOrDefault(keyword, 0);
            if (occurrences == 0)
            {
                throw new IllegalStateException("Unexpected or excess keyword occurrence for " + page.title());
            }
            if (occurrences == 1)
            {
                remaining.remove(keyword);
            }
            else
            {
                remaining.put(keyword, occurrences - 1);
            }
            StringJoiner text = new StringJoiner(" ");
            for (int wordNo = 0; wordNo < shifts.getWordCount(lineNo); wordNo++)
            {
                text.add(shifts.getWord(lineNo, wordNo));
            }
            entries.add(new Object[] { keyword, text.toString(), page.title() });
        }
        if (remainingWords.values().stream().anyMatch(words -> !words.isEmpty()))
        {
            throw new IllegalStateException("KWIC output is incomplete; the existing index was not replaced.");
        }
        return entries;
    }
}
