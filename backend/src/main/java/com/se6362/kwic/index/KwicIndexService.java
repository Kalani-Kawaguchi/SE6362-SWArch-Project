package com.se6362.kwic.index;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.se6362.kwic.core.Alphabetizer;
import com.se6362.kwic.core.CircularShift;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.core.Rotation;

@Service
public class KwicIndexService
{
    private static final int MAX_URLS = 20;
    // Must not exceed KwicLine.uri's column length (768).
    private static final int MAX_URL_LENGTH = 768;

    private final UrlFetcher fetcher;
    private final HtmlTextExtractor extractor;
    private final KwicLineRepository repository;

    public KwicIndexService(
            UrlFetcher fetcher,
            HtmlTextExtractor extractor,
            KwicLineRepository repository)
    {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.repository = repository;
    }

    @Transactional
    public IndexResult index(List<String> urls)
    {
        List<IndexResult.Indexed> indexed = new ArrayList<>();
        List<IndexResult.Failed> failed = new ArrayList<>();
        int totalRotations = 0;

        List<String> limited = urls.size() > MAX_URLS
                ? urls.subList(0, MAX_URLS)
                : urls;

        for (String url : limited)
        {
            if (url == null || url.isBlank())
            {
                failed.add(new IndexResult.Failed(url, "blank URL"));
                continue;
            }
            if (url.length() > MAX_URL_LENGTH)
            {
                failed.add(new IndexResult.Failed(url, "URL too long"));
                continue;
            }

            try
            {
                String html = fetcher.fetch(url);
                List<String> lines = extractor.extractLines(html);

                if (lines.isEmpty())
                {
                    failed.add(new IndexResult.Failed(url, "no indexable text found"));
                    continue;
                }

                List<Rotation> rotations = buildRotations(url, lines);

                repository.deleteByUriIn(List.of(url));
                List<KwicLine> rows = new ArrayList<>();
                for (Rotation rotation : rotations)
                {
                    rows.add(new KwicLine(
                            rotation.uri(),
                            rotation.firstWord(),
                            rotation.text()));
                }
                repository.saveAll(rows);

                indexed.add(new IndexResult.Indexed(url, lines.size()));
                totalRotations += rows.size();
            }
            catch (FetchException e)
            {
                failed.add(new IndexResult.Failed(url, e.getMessage()));
            }
        }

        return new IndexResult(indexed, failed, totalRotations);
    }

    private List<Rotation> buildRotations(String url, List<String> lines)
    {
        LineStorage storage = new LineStorage();
        URI uri = URI.create(url);
        for (String line : lines)
        {
            storage.storeLine(line, uri);
        }
        CircularShift shifts = new CircularShift();
        shifts.circularShiftLines(storage);
        Alphabetizer alphabetizer = new Alphabetizer();
        alphabetizer.alphabetizeLines(shifts);
        return alphabetizer.getRotations();
    }
}
