package com.se6362.kwic.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.se6362.kwic.BackendApplication;
import com.se6362.kwic.core.MasterControl;
import com.se6362.kwic.database.IndexDatabase;

public final class BuildIndex {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BuildIndex() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            throw new IllegalArgumentException("Usage: BuildIndex");
        }
        SpringApplication application = new SpringApplication(BackendApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (var context = application.run("--spring.jpa.hibernate.ddl-auto=none", "--spring.sql.init.mode=never")) {
            int count = build(context.getBean(IndexDatabase.class));
            System.out.println("Saved " + count + " shifts to INDEX_ENTRY. WEB_PAGE was not changed.");
        }
    }

    static int build(IndexDatabase database) throws IOException {
        var pages = database.getPages();
        if (pages.isEmpty()) {
            throw new IllegalStateException("No WEB_PAGE entries found.");
        }
        var input = Files.createTempFile("kwic-build-", ".jsonl");
        try {
            try (var writer = Files.newBufferedWriter(input, StandardCharsets.UTF_8)) {
                for (var page : pages) {
                    writer.write(MAPPER.writeValueAsString(page));
                    writer.newLine();
                }
            }
            MasterControl control = new MasterControl();
            control.runIncremental(input);
            return control.saveOutput(database);
        } finally {
            Files.deleteIfExists(input);
        }
    }
}
