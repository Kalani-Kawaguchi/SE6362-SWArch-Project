/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: SearchController.java
    Description: REST controller mapping POST requests sent from search engine
 */

package com.se6362.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;

import com.se6362.database.DatabaseManager;
import com.se6362.kwic.JsonReader;
import com.se6362.search.PageRankStrategy;
import com.se6362.search.RankByAccessFrequency;
import com.se6362.search.RankByTitle;
import com.se6362.webpage.PageList;
import com.se6362.webpage.WebPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class SearchController
{
    private final DatabaseManager database;
    private final ObjectMapper objectMapper;

    public SearchController(DatabaseManager database)
    {
        this.database = database;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getSearchResults(
            @RequestParam String query,
            @RequestParam(defaultValue = "false") boolean isCaseSensitive,
            @RequestParam(defaultValue = "title") String rankingStrategy)
            throws JsonProcessingException
    {
        PageRankStrategy pageRankStrategy;

        if (rankingStrategy.equals("title"))
        {
            pageRankStrategy = new RankByTitle();
        }
        else if (rankingStrategy.equals("access_frequency"))
        {
            pageRankStrategy = new RankByAccessFrequency();
        }
        else
        {
            throw new IllegalArgumentException("The given page rank strategy is unavailable");
        }

        PageList results = database.getSearchResults(
                query,
                pageRankStrategy,
                isCaseSensitive);

        return objectMapper.writeValueAsString(results.toJsonArray());
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updatePageAccessFrequency(@RequestBody JsonNode element) throws IOException
    {
        if (!element.isObject() || element.get("access_frequency") == null)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webpage object");
        }

        WebPage page = JsonReader.parseJson(element);

        database.incrementAccessFrequency(page);
    }
}
