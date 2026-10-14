package com.se6362.kwic.importer;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

import com.se6362.kwic.BackendApplication;

public final class ImportWebPages {
    private ImportWebPages() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ImportWebPages <webpages.jsonl>");
        }
        SpringApplication application = new SpringApplication(BackendApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (var context = application.run("--spring.jba.hibernate.ddl-auto=none", "--spring.sql.init.mode=never")) {
            int count = context.getBean(WebPageImportService.class).importFile(Path.of(args[0]));
            System.out.println("Imported " + count + " original webpage records into WEB_PAGE.");
        }
    }
}
