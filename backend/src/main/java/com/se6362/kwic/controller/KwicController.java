package com.se6362.kwic.controller;

import com.se6362.kwic.core.*;

import tools.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class KwicController {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping("/kwic")
    public KwicResponse receiveInput(@RequestBody KwicRequest input) throws IOException {
        if (input.text() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text required");
        }

        System.out.println("KWIC Input Received: " + input.text());

        Path inputFile = Files.createTempFile("kwic-", ".jsonl");
        try{
            try (BufferedWriter writer = Files.newBufferedWriter(inputFile)) {
                for (String line : input.text().split("\\R")){
                    if (line.isBlank()){
                        continue;
                    }

                    String jsonLine = MAPPER.writeValueAsString(new KwicRequest(line));
                    System.out.println("KWIC Input Record: " + jsonLine);
                    writer.write(jsonLine);
                    writer.newLine();
                }
            }

            MasterControl masterControl = new MasterControl();
            masterControl.runIncremental(inputFile);
            String sortedShifts = masterControl.getOutput();

            System.out.println("KWIC sorted output:\n" + sortedShifts);
            return new KwicResponse(input.text(), sortedShifts);
        } finally {
            Files.deleteIfExists(inputFile);
        }
    }

    public record KwicRequest(String text) {}

    public record KwicResponse(String text, String sortedShifts) {}
}
