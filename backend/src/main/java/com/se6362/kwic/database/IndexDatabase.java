/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: IndexDatabase.java
    Description: Module to communicate with KWIC index SQL database
 */

package com.se6362.kwic.database;

import java.net.URI;
import java.net.URISyntaxException;
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

import com.se6362.kwic.core.PageList;
import com.se6362.kwic.core.WebPage;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.core.Descriptor;
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
    public int replaceIndex(PageList shifts)
    {
        List<WebpageRecord> pages = database.query(SELECT_PAGES + " FOR UPDATE", PAGE_MAPPER);
        List<Object[]> entries;
        try
        {
            entries = prepareEntries(shifts, pages);
        }
        catch (URISyntaxException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
        database.update("DELETE FROM INDEX_ENTRY");
        database.batchUpdate("INSERT INTO INDEX_ENTRY (Keyword, ShiftText, PageTitle) VALUES (?, ?, ?)", entries);
        return entries.size();
    }

    // Format database entries for insertion
    private static List<Object[]> prepareEntries(PageList shifts, List<WebpageRecord> pages) throws URISyntaxException
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
            if (page.text().isBlank())
            {
                throw new IllegalStateException("WEB_PAGE entry has no text: " + page.title());
            }
            Descriptor descriptor = new Descriptor(page.text());
            Map<String, Integer> frequencies = new HashMap<>();
            for (int wordNo = 0; wordNo < descriptor.getLength(); wordNo++)
            {
                frequencies.merge(descriptor.getWord(wordNo), 1, Integer::sum);
            }
            remainingWords.put(uri, frequencies);
            wordCounts.put(uri, descriptor.getLength());
        }
        List<Object[]> entries = new ArrayList<>();
        for (WebPage shift : shifts)
        {
            URI uri = new URI(shift.getUrl());
            WebpageRecord page = pagesByUri.get(uri);
            if (page == null || shift.getDescriptor().getLength() != wordCounts.get(uri))
            {
                throw new IllegalStateException("A shift has an unknown URL or incorrect word count.");
            }
            String keyword = shift.getDescriptor().getWord(0);
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
            entries.add(new Object[] { keyword, shift.getDescriptor().toString(), page.title() });
        }
        if (remainingWords.values().stream().anyMatch(words -> !words.isEmpty()))
        {
            throw new IllegalStateException("KWIC output is incomplete; the existing index was not replaced.");
        }
        return entries;
    }
}
