package com.se6362.kwic.core;

import java.io.IOException;
import java.io.FileReader;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;

public class InputBuffer implements AutoCloseable
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final JsonParser parser;

    public InputBuffer(Path filePath) throws IOException
    {
        this.parser = MAPPER.createParser(new FileReader(filePath.toString()));
    }

    /**
     * Thread-safe method to read a JSON element.
     * Returns an empty list when the file is fully processed.
     */
    public synchronized LineStorage getInputLine() throws IOException
    {
        LineStorage storage = new LineStorage();

        JsonNode element;
        while ((element = MAPPER.readTree(parser)) != null)
        {
            storage.setLine(element.path("text").asText());
            if (storage.getLineCount() > 0)
            {
                return storage;
            }
        }

        return storage;
    }

    @Override
    public synchronized void close() throws IOException
    {
        if (parser != null)
        {
            parser.close();
        }
    }
}
