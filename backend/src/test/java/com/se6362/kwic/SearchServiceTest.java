package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.se6362.kwic.index.KwicLine;
import com.se6362.kwic.index.KwicLineRepository;
import com.se6362.kwic.index.SearchResult;
import com.se6362.kwic.index.SearchService;

@DataJpaTest
@Import(SearchService.class)
public class SearchServiceTest
{
    @Autowired
    KwicLineRepository repository;

    @Autowired
    SearchService service;

    @Test
    void groupsResultsByUrlPreservingOrder()
    {
        repository.save(new KwicLine("http://b", "software", "software architecture is"));
        repository.save(new KwicLine("http://b", "software", "software engineering is"));
        repository.save(new KwicLine("http://d", "software", "software systems design"));

        SearchResult result = service.search("soft");

        assertEquals(3, result.resultCount());
        assertEquals(2, result.groups().size());
        assertEquals("http://b", result.groups().get(0).url());
        assertEquals(2, result.groups().get(0).contexts().size());
        assertEquals("http://d", result.groups().get(1).url());
    }

    @Test
    void emptyQueryReturnsNoResults()
    {
        SearchResult result = service.search("  ");
        assertEquals(0, result.resultCount());
        assertEquals(0, result.groups().size());
    }
}
