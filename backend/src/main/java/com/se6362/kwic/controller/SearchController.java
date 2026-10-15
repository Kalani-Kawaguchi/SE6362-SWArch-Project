/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: SearchController.java
    Description: REST controller mapping POST requests sent from search engine
 */

package com.se6362.kwic.controller;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.database.IndexDatabase;

@RestController
@RequestMapping("/api")
public class SearchController
{
    private static final int MAX_QUERY_LENGTH = 500;
    private final IndexDatabase database;

    public SearchController(IndexDatabase database)
    {
        this.database = database;
    }

    // Takes keywords from user and returns search results that match
    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody SearchRequest request)
    {
        if (request == null || request.query() == null || request.query().isEmpty()
                || request.query().size() > MAX_QUERY_LENGTH)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an nonempty query");
        }
        int length = request.query().size() - 1;
        for (String term : request.query())
        {
            if (term == null || term.length() > MAX_QUERY_LENGTH)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each query term must be a string of at most 500 chars.");
            }
            length += term.length();
        }
        if (length > MAX_QUERY_LENGTH)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total query length must be at most 500 chars.");
        }
        LineStorage input = new LineStorage();
        input.storeLine(String.join(" ", request.query()));
        if (input.getLineCount() == 0)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one searchable word");
        }
        Set<String> keywords = new LinkedHashSet<>();
        for (int wordNo = 0; wordNo < input.getWordCount(0); wordNo++)
        {
            keywords.add(input.getWord(0, wordNo).toLowerCase(Locale.ROOT));
        }
        return database.search(List.copyOf(keywords)).stream()
                .map(page -> new SearchResult(page.title(), page.url(), page.text()))
                .toList();
    }

    public record SearchRequest(List<String> query)
    {
    }

    public record SearchResult(String title, String url, String description)
    {
    }
}
