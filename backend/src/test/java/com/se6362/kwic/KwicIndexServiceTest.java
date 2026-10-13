package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.se6362.kwic.index.FetchException;
import com.se6362.kwic.index.HtmlTextExtractor;
import com.se6362.kwic.index.IndexResult;
import com.se6362.kwic.index.KwicIndexService;
import com.se6362.kwic.index.KwicLine;
import com.se6362.kwic.index.KwicLineRepository;
import com.se6362.kwic.index.UrlFetcher;

@DataJpaTest
@Import({ KwicIndexService.class, HtmlTextExtractor.class })
public class KwicIndexServiceTest
{
    @Autowired
    KwicLineRepository repository;

    @Autowired
    KwicIndexService service;

    @MockitoBean
    UrlFetcher fetcher;

    @Test
    void indexesAndPersistsRotationsWithUrls() throws FetchException
    {
        when(fetcher.fetch("http://a")).thenReturn("<p>quick brown fox</p>");

        IndexResult result = service.index(List.of("http://a"));

        assertEquals(1, result.indexed().size());
        assertEquals("http://a", result.indexed().get(0).url());
        // "quick brown fox" -> 3 rotations
        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("brown");
        assertEquals(1, hits.size());
        assertEquals("http://a", hits.get(0).getUri());
        assertEquals("brown fox quick", hits.get(0).getLineText());
    }

    @Test
    void reindexingSameUrlReplacesRows() throws FetchException
    {
        when(fetcher.fetch("http://a")).thenReturn("<p>alpha beta</p>");
        service.index(List.of("http://a"));

        when(fetcher.fetch("http://a")).thenReturn("<p>gamma delta</p>");
        service.index(List.of("http://a"));

        assertTrue(repository
                .findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("alpha").isEmpty());
        assertEquals(1, repository
                .findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("gamma").size());
    }

    @Test
    void failedFetchIsReportedNotThrown() throws FetchException
    {
        when(fetcher.fetch("http://bad")).thenThrow(new FetchException("timeout"));

        IndexResult result = service.index(List.of("http://bad"));

        assertTrue(result.indexed().isEmpty());
        assertEquals(1, result.failed().size());
        assertEquals("http://bad", result.failed().get(0).url());
    }

    @Test
    void nullUrlIsReportedNotThrown()
    {
        List<String> urls = new ArrayList<>();
        urls.add(null);

        IndexResult result = service.index(urls);

        assertTrue(result.indexed().isEmpty());
        assertEquals(1, result.failed().size());
    }

    @Test
    void urlsBeyondBatchLimitAreReportedNotDropped() throws FetchException
    {
        when(fetcher.fetch(anyString())).thenThrow(new FetchException("stub"));

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < 25; i++)
        {
            urls.add("http://example.com/" + i);
        }

        IndexResult result = service.index(urls);

        // Every URL is accounted for: 20 attempted (and stubbed to fail) + 5 over-limit.
        assertEquals(25, result.failed().size());
        assertTrue(result.failed().stream()
                .anyMatch(f -> f.error().contains("exceeds max batch")));
    }
}
