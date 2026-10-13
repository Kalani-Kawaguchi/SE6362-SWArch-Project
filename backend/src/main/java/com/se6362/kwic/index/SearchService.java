package com.se6362.kwic.index;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SearchService
{
    private final KwicLineRepository repository;

    public SearchService(KwicLineRepository repository)
    {
        this.repository = repository;
    }

    public SearchResult search(String query)
    {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty())
        {
            return new SearchResult(q, 0, List.of());
        }

        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc(q);

        Map<String, List<String>> byUrl = new LinkedHashMap<>();
        for (KwicLine hit : hits)
        {
            byUrl.computeIfAbsent(hit.getUri(), k -> new ArrayList<>())
                    .add(hit.getLineText());
        }

        List<SearchResult.Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byUrl.entrySet())
        {
            groups.add(new SearchResult.Group(entry.getKey(), entry.getValue()));
        }

        return new SearchResult(q, hits.size(), groups);
    }
}
