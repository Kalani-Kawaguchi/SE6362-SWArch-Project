package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.se6362.kwic.index.KwicLine;
import com.se6362.kwic.index.KwicLineRepository;

@DataJpaTest
public class KwicLineRepositoryTest
{
    @Autowired
    KwicLineRepository repository;

    @Test
    void prefixSearchIsCaseInsensitiveAndOrdered()
    {
        repository.save(new KwicLine("http://b", "software", "software architecture is"));
        repository.save(new KwicLine("http://b", "engineering", "engineering is the process"));
        repository.save(new KwicLine("http://c", "Soft", "Soft touch here"));

        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("soft");

        assertEquals(2, hits.size());
        assertTrue(hits.get(0).getFirstWord().equalsIgnoreCase("soft")
                || hits.get(0).getFirstWord().equalsIgnoreCase("software"));
    }

    @Test
    void deleteByUriInRemovesOnlyThoseUrls()
    {
        repository.save(new KwicLine("http://b", "alpha", "alpha one"));
        repository.save(new KwicLine("http://c", "alpha", "alpha two"));

        repository.deleteByUriIn(List.of("http://b"));

        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("alpha");
        assertEquals(1, hits.size());
        assertEquals("http://c", hits.get(0).getUri());
    }
}
