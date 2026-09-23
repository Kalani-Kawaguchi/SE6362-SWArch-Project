package com.se6362.kwic.InputManager;

import com.se6362.kwic.LineList.StorageLineList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InputManager implements AutoCloseable
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final BufferedReader reader;
    private boolean isEof = false;

    public InputManager(String filePath) throws IOException
    {
        this.reader = new BufferedReader(new FileReader(filePath));
    }

    /**
     * Thread-safe method to read a batch of lines.
     * Returns an empty list when the file is fully processed.
     */
    public synchronized StorageLineList getNextBatch(int batchSize) throws IOException
    {
        StorageLineList batch = new StorageLineList();

        if (isEof)
        {
            return batch; // Return empty list immediately if already at EOF
        }

        String line;
        while (batch.getLineCount() < batchSize && (line = reader.readLine()) != null)
        {
            batch.addLine((this.parseJsonLine(line)));
        }

        if (batch.getLineCount() < batchSize)
        {
            isEof = true; // Mark EOF if we couldn't completely fill the final batch
        }

        return batch;
    }

    @Override
    public synchronized void close() throws IOException
    {
        if (reader != null)
        {
            reader.close();
        }
    }

    private String parseJsonLine(String jsonLine)
    {
        try
        {
            // 1. Parse the raw line string into a readable tree structure
            JsonNode rootNode = MAPPER.readTree(jsonLine);

            // 2. Safely extract only the content under the "text" key

            return rootNode.path("text").asText();

        }
        catch (Exception e)
        {
            System.err.println("Failed to parse JSON line: " + e.getMessage());
            return "";
        }
    }
}
