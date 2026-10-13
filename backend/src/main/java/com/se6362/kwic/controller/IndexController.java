package com.se6362.kwic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.se6362.kwic.index.IndexResult;
import com.se6362.kwic.index.KwicIndexService;
import com.se6362.kwic.index.SearchResult;
import com.se6362.kwic.index.SearchService;

@CrossOrigin(origins = { "https://kalani-kawaguchi.github.io", "http://localhost:1234" })
@RestController
@RequestMapping("/api")
public class IndexController
{
    private final KwicIndexService indexService;
    private final SearchService searchService;

    public IndexController(KwicIndexService indexService, SearchService searchService)
    {
        this.indexService = indexService;
        this.searchService = searchService;
    }

    @PostMapping("/index")
    public IndexResult index(@RequestBody IndexRequest request)
    {
        if (request == null || request.urls() == null || request.urls().isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one URL is required");
        }
        return indexService.index(request.urls());
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam(name = "q", required = false) String q)
    {
        if (q == null || q.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query 'q' is required");
        }
        return searchService.search(q);
    }

    public record IndexRequest(List<String> urls)
    {
    }
}
