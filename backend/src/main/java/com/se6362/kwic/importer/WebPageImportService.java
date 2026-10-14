package com.se6362.kwic.importer;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import com.se6362.kwic.importer.TsvToJsonl.WebpageRecord;

@Service
public class WebPageImportService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final JdbcTemplate database;

    public WebPageImportService(JdbcTemplate database) {
        this.database = database;
    }

    @Transactional(rollbackFor = IOException.class)
    public int importFile(Path input) throws IOException {
        List<WebpageRecord> pages = readPages(input);
        List<Object[]> values = pages.stream()
            .map(page -> new Object[] { page.url(), page.title(), page.text() })
            .toList();
        database.batchUpdate("INSERT INTO WEB_PAGE (Url, Title, Description) VALUES (?, ?, ?)", values);
        return values.size();
    }

    static List<WebpageRecord> readPages(Path input) throws IOException {
        List<WebpageRecord> pages = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        Set<String> urls = new HashSet<>();
        try (JsonParser parser = MAPPER.createParser(Files.newBufferedReader(input, StandardCharsets.UTF_8))) {
            JsonNode node;
            while ((node = MAPPER.readTree(parser)) != null) {
                int recordNumber = pages.size() + 1;
                String context = "JSON record " + recordNumber + ": ";
                String url = requiredString(node, "url", context);
                String title = requiredString(node, "title", context);
                String description = requiredString(node, "text", context);
                if (title.codePointCount(0, title.length()) > 512 || !title.equals(title.stripTrailing())) {
                    throw new IOException(context + "Title is too long or has trailing whitespace.");
                }
                try {
                    URI uri = URI.create(url);
                    if ((!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) || uri.getHost() == null) {
                        throw new IllegalArgumentException("URL must use http or https scheme.");
                    }
                } catch (IllegalArgumentException e) {
                    throw new IOException(context + "Invalid URL: " + url, e);
                }
                if (!titles.add(title) || !urls.add(url)) {
                    throw new IOException(context + "Duplicate title or URL");
                }
                pages.add(new WebpageRecord(url, title, description));
            }
        }
        if (pages.isEmpty()) {
            throw new IOException("Input contains no webpage records.");
        }
        return List.copyOf(pages);
    }

    private static String requiredString(JsonNode node, String field, String context) throws IOException {
        JsonNode value = node.get(field);
        if (!node.isObject() || value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IOException(context + field + " must be a nonblank string.");
        }
        return value.asText();
    }
}
