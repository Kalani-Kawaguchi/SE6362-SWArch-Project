package com.se6362.kwic.controller;

import com.se6362.kwic.core.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.node.ObjectNode;

@CrossOrigin(origins = "http://localhost:1234")
@RestController
@RequestMapping("/api")
public class KwicController
{
    MasterControl masterControl = new MasterControl();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping("/kwic")
    public synchronized String receiveInput(@RequestBody KwicRequest input) throws IOException
    {
        if (input.text() == null)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text required");
        }

        System.out.println("KWIC Input Received: " + input.text());

        Path inputFile = Files.createTempFile("kwic-", ".jsonl");
        try
        {
            try (BufferedWriter writer = Files.newBufferedWriter(inputFile))
            {
                for (String line : input.text().split("\\R"))
                {
                    if (line.isBlank())
                    {
                        continue;
                    }

                    String jsonLine = MAPPER.writeValueAsString(new KwicRequest(line));
                    System.out.println("KWIC Input Record: " + jsonLine);
                    writer.write(jsonLine);
                    writer.newLine();
                }
            }

            masterControl.runIncremental(inputFile);

            ObjectNode json = masterControl.toJsonElement();
            String sortedShifts = masterControl.getOutput();

            System.out.println("KWIC JSON:\n" + json.toPrettyString());
            System.out.println("KWIC sorted output:\n" + sortedShifts);

            return json.toString();
        }
        finally
        {
            Files.deleteIfExists(inputFile);
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public synchronized void clear()
    {
        masterControl.clear();
    }

    public record KwicRequest(String text)
    {
    }

    // public record KwicResponse(ObjectNode jsonObject) {}
}
