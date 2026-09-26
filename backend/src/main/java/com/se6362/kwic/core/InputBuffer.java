package com.se6362.kwic.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InputBuffer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private final LineStorage lineStorage;
    private int inputLineNumber = 0;

    public InputBuffer(LineStorage lineStorage) {
        this.lineStorage = Objects.requireNonNull(lineStorage);
    }

    public boolean getInputLine(BufferedReader reader) throws IOException {
        String jsonLine;

        while ((jsonLine = reader.readLine()) != null) {
            inputLineNumber++;

            if (jsonLine.isBlank()) {
                continue; // Skip blank lines
            }

            String inputLine = WHITESPACE.matcher(parseJson(jsonLine)).replaceAll(" ").strip();
            if (inputLine.isEmpty()) {
                continue; // Skip lines that are empty
            }

            storeInput(inputLine);
            return true;
        }

        lineStorage.clear(); // clear the storage when the end of file reached
        return false; // End of file
    }

    private void storeInput(String inputLine) {
        lineStorage.clear();
        String[] words = inputLine.split(" ");

        for (int wordNumber = 0; wordNumber < words.length; wordNumber++) {
            lineStorage.setWord(0, wordNumber, words[wordNumber]);
        }
    }

    private String parseJson(String jsonLine) throws IOException {
        JsonNode record;

        try {
            record = MAPPER.readTree(jsonLine);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to parse JSON line " + inputLineNumber + ": " + jsonLine + ".", e);
        }

        if (record == null || !record.isObject() || !record.path("text").isTextual()) {
            throw new IOException("Input line " + inputLineNumber + " must be an object with a text string.");
        }

        return record.get("text").asText();
    }
}
