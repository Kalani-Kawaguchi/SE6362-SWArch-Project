package com.se6362.kwic.importer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class TsvToJsonl {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CSVFormat TSV = CSVFormat.DEFAULT.builder()
            .setDelimiter('\t').setIgnoreEmptyLines(true).build();
    
    private TsvToJsonl(){}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: TsvToJsonl <input.tsv> <output.jsonl>");
        }
        int count = convert(Path.of(args[0]), Path.of(args[1]));
        System.out.println("Converted " + count + " webpage entries to " + args[1]);
    }

    public static int convert(Path input, Path output) throws IOException {
        Path destination = output.toAbsolutePath();
        if (Files.exists(destination)) {
            throw new IOException("Output already exists: " + destination);
        }
        Path temporary = Files.createTempFile(destination.getParent(), "kwic-import-", ".jsonl");
        int count = 0;
        try {
            try (Reader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
                    CSVParser parser = TSV.parse(reader);
                    BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
            {
                for (CSVRecord row : parser) {
                    WebpageRecord record = parse(row);
                    writer.write(MAPPER.writeValueAsString(record));
                    writer.newLine();
                    count++;
                }
            }
            if (count == 0) {
                throw new IllegalArgumentException("Input contains no webpage entries.");
            }
            Files.move(temporary, destination);
            return count;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static WebpageRecord parse(CSVRecord row) {
        String context = "TSV record " + row.getRecordNumber() + ": ";
        if (row.size() != 3) {
            throw new IllegalArgumentException(context + "Expected URL, title, description (3 columns).");
        }
        String url = row.get(0);
        String title = row.get(1);
        String text = row.get(2);
        if (url.isBlank() || title.isBlank() || text.isBlank()) {
            throw new IllegalArgumentException(context + "URL, title, and description must not be blank.");
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(context + "Invalid URL: " + url, e);
        }
        if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(context + "URL must use http or https scheme.");
        }
        return new WebpageRecord(url, title, text);
    }

    public record WebpageRecord(String url, String title, String text) {}
}
