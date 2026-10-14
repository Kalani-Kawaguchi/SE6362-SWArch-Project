package com.se6362.kwic.controller;

import com.se6362.kwic.core.MasterControl;
import com.se6362.kwic.core.SearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SearchController
{
    private final MasterControl index = new MasterControl();

    public SearchController() throws IOException
    {
        Path dataFile = Files.createTempFile("webpages-", ".jsonl");
        try (InputStream in = new ClassPathResource("webpages.jsonl").getInputStream())
        {
            Files.copy(in, dataFile, StandardCopyOption.REPLACE_EXISTING);
            index.runIncremental(dataFile);
        }
        finally
        {
            Files.deleteIfExists(dataFile);
        }
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam("q") String query)
    {
        List<SearchResult> results = new ArrayList<>();

        String cleaned = query.replaceAll("[^\\p{L}\\p{N}\\p{javaWhitespace}]", "").trim();
        if (cleaned.isEmpty())
        {
            return results;
        }

        for (String keyword : cleaned.split("\\p{javaWhitespace}+"))
        {
            results.addAll(index.search(keyword));
        }

        return results;
    }
}