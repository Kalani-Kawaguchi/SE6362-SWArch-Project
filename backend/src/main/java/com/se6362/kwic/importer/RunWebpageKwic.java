package com.se6362.kwic.importer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.core.MasterControl;
import com.se6362.kwic.importer.TsvToJsonl.WebpageRecord;

public final class RunWebpageKwic {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RunWebpageKwic(){}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java RunWebpageKwic <webpages.jsonl> <report.json>");
        }
        ObjectNode report = run(Path.of(args[0]), Path.of(args[1]));
        System.out.println("Processed " + report.get("pageCount").asInt() + " pages into " + report.get("shiftCount").asInt() + " sorted shifts.");
        System.out.println("Report saved to " + args[1] + ". No database rows changed.");
    }

    public static ObjectNode run(Path input, Path output) throws IOException {
        Path destination = output.toAbsolutePath();
        if (Files.exists(destination)) {
            throw new IllegalArgumentException("Report already exists: " + destination);
        }
        var pages = WebPageImportService.readPages(input);
        Map<URI, WebpageRecord> pagesByUri = new HashMap<>();
        for (WebpageRecord page : pages) {
            if (pagesByUri.put(URI.create(page.url()), page) != null) {
                throw new IOException("Multiple webpage records have the same URI: " + page.url());
            }
        }
        MasterControl control = new MasterControl();
        control.runIncremental(input);
        LineStorage shifts = control.getOutputLines();
        ObjectNode report = control.toJsonElement().deepCopy();
        report.put("pageCount", pages.size());
        report.put("shiftCount", shifts.getLineCount());
        report.set("webpages", MAPPER.valueToTree(pages));
        var entries = report.putArray("sortedEntries");
        for (int lineNo = 0; lineNo < shifts.getLineCount(); lineNo++) {
            WebpageRecord page = pagesByUri.get(shifts.getURI(lineNo));
            if (page == null) {
                throw new IOException("A circular shift lost its webpage association.");
            }
            StringJoiner text = new StringJoiner(" ");
            for (int wordNo = 0; wordNo < shifts.getWordCount(lineNo); wordNo++) {
                text.add(shifts.getWord(lineNo, wordNo));
            }
            entries.addObject()
                .put("text", text.toString())
                .put("url", page.url())
                .put("title", page.title());
        }
        Path temporary = Files.createTempFile(destination.getParent(), "kwic-report-", ".json");
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), report);
            Files.move(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return report;
    }
}
