/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: SearchController.java
    Description: REST controller mapping POST requests sent from search engine
 */

package com.se6362.search;

import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import com.se6362.index.JsonReader;
import com.se6362.web.PageList;
import com.se6362.web.WebPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://localhost:1234")
@RestController
@RequestMapping("/api/search")
public class SearchController
{
    private final DatabaseManager database;
    private final ObjectMapper objectMapper;
    private static final int SUGGESTION_LIMIT = 8;
    private static final Set<String> OPERATORS = Set.of("AND", "OR", "NOT");

    public SearchController(DatabaseManager database)
    {
        this.database = database;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
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

        QueryParser queryParser = new QueryParser(query);

        PageList results = database.getSearchResults(
                queryParser.getParseTreeRoot(),
                pageRankStrategy,
                isCaseSensitive);

        return objectMapper.writeValueAsString(results.toJsonArray());
    }

    @GetMapping("/suggestions")
    public List<String> getSuggestions(@RequestParam String term, @RequestParam(defaultValue = "false") boolean isCaseSensitive)
    {
        if (term.length() > 50)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Suggestion terms must be at most 50 characters.");
        }

        if (!isSuggestionWord(term))
        {
            return List.of();
        }

        String typed = normalizeSuggestion(term, isCaseSensitive);

        List<String> suggestions = new ArrayList<>(
                database.getKeywordsStartingWith(term, isCaseSensitive)
                        .stream()
                        .filter(this::isSuggestionWord)
                        .filter(word -> normalizeSuggestion(word, isCaseSensitive)
                                .startsWith(typed))
                        .distinct()
                        .sorted()
                        .limit(SUGGESTION_LIMIT)
                        .toList());
        
        if (suggestions.size() == SUGGESTION_LIMIT)
        {
            return suggestions;
        }

        int allowedErrors = term.length() >= 5 ? 2 : 1;

        List<String> corrections = database
                .getTypoCandidates(term.length(), allowedErrors)
                .stream()
                .filter(this::isSuggestionWord)
                .filter(word -> !suggestions.contains(word))
                .distinct()
                .map(word -> Map.entry(
                        word,
                        typoDistance(
                                typed,
                                normalizeSuggestion(word, isCaseSensitive))))
                .filter(candidate -> candidate.getValue() <= allowedErrors)
                .sorted(Comparator
                        .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .limit(SUGGESTION_LIMIT - suggestions.size())
                .map(Map.Entry::getKey)
                .toList();

        suggestions.addAll(corrections);
        return suggestions;
    }

    private boolean isSuggestionWord(String word)
    {
        return word != null
                && word.length() >= 2
                && word.length() <= 50
                && !OPERATORS.contains(word)
                && word.chars().noneMatch(character ->
                        Character.isWhitespace(character)
                                || character == '('
                                || character == ')');
    }

    private String normalizeSuggestion(String word, boolean caseSensitive)
    {
        return caseSensitive ? word : word.toLowerCase(Locale.ROOT);
    }

    private int typoDistance(String first, String second)
    {
        int[][] costs = new int[first.length() + 1][second.length() + 1];

        for (int i = 0; i <= first.length(); i++)
        {
            costs[i][0] = i;
        }

        for (int j = 0; j <= second.length(); j++)
        {
            costs[0][j] = j;
        }

        for (int i = 1; i <= first.length(); i++)
        {
            for (int j = 1; j <= second.length(); j++)
            {
                int replacement = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;

                costs[i][j] = Math.min(
                        Math.min(costs[i - 1][j] + 1, costs[i][j - 1] + 1),
                        costs[i - 1][j - 1] + replacement);
                
                if (i > 1 && j > 1 && first.charAt(i - 1) == second.charAt(j - 2) && first.charAt(i - 2) == second.charAt(j - 1))
                {
                    costs[i][j] = Math.min(costs[i][j], costs[i - 2][j - 2] + 1);
                }
            }
        }

        return costs[first.length()][second.length()];
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public String updatePageAccessFrequency(@RequestBody Map<String, Object> payload) throws IOException
    {
        // 1. Validate that the payload has the required keys
        if (payload == null
                || payload.get("title") == null
                || payload.get("url") == null
                || payload.get("description") == null)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webpage object");
        }

        // 2. Safely bridge back to JsonNode for your existing JsonReader logic
        JsonNode element = objectMapper.valueToTree(payload);

        WebPage page = JsonReader.parseJson(element);
        database.incrementAccessFrequency(page);
        return "successful";
    }

}
