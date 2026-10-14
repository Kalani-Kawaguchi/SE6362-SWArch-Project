package com.se6362.kwic.core;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InputBuffer implements AutoCloseable
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final InputLine END_OF_FILE = new InputLine("", null);

    private final BlockingQueue<InputLine> queue = new LinkedBlockingQueue<>();

    private Thread readerThread;
    private volatile IOException readException;

    public void readFromFile(Path filePath) throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            throw new IOException("A file is already being read.");
        }

        queue.clear();
        readException = null;

        readerThread = new Thread(() ->
        {
            try (JsonParser parser = MAPPER.createParser(
                    Files.newBufferedReader(filePath, StandardCharsets.UTF_8)))
            {
                JsonNode element;

                while ((element = MAPPER.readTree(parser)) != null)
                {
                    InputLine input = parseInput(element);
                    if (!input.text().isBlank()) {
                        queue.put(input);
                    }
                }
            }
            catch (IOException exception)
            {
                readException = exception;
            }
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
            }
            finally
            {
                try
                {
                    queue.put(END_OF_FILE);
                }
                catch (InterruptedException exception)
                {
                    Thread.currentThread().interrupt();
                }
            }
        });

        readerThread.start();
    }

    public LineStorage getInputLines(int numberOfLines) throws IOException
    {
        LineStorage storage = new LineStorage();

        try
        {
            for (int i = 0; i < numberOfLines; i++)
            {
                InputLine next = queue.take();

                if (next == END_OF_FILE)
                {
                    // Make EOF available to other workers.
                    queue.put(END_OF_FILE);

                    if (readException != null)
                    {
                        throw readException;
                    }

                    break;
                }

                // Add the line(s) from next to storage.
                storage.storeLine(next.text(), next.uri());
            }

            return storage;
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Thread interrupted while waiting for input.",
                    exception);
        }
    }

    private static InputLine parseInput(JsonNode element) throws IOException {
        if (!element.isObject() || !element.path("text").isTextual()){
            throw new IOException("Each input record must contain a string text field.");
        }
        JsonNode url = element.get("url");
        if (url == null || url.isNull()){
            return new InputLine(element.get("text").asText(), null);
        }
        if (!url.isTextual()){
            throw new IOException("The url field must be a string.");
        }
        try {
            URI uri = URI.create(url.asText());
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IOException("The url field must use http or https scheme.");
            }
            return new InputLine(element.get("text").asText(), uri);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid webpage URL: " + url.asText(), e);
        }
    }

    private record InputLine(String text, URI uri) {}

    @Override
    public void close() throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            readerThread.interrupt();
        }
    }
}