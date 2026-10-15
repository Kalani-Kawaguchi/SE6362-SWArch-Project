/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: InputBuffer.java
    Description: Reads JSON file containing lines of text and temporarily stores lines prior to KWIC index generation.
 */

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

    // Reads in data from file in a separate thread. Use getInputLines to get a line
    // of input that was read.
    public void readFromFile(Path filePath) throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            throw new IOException("A file is already being read.");
        }

        queue.clear();
        readException = null;

        // Creates new thread to read file.
        readerThread = new Thread(() ->
        {
            try (JsonParser parser = MAPPER.createParser(
                    Files.newBufferedReader(filePath, StandardCharsets.UTF_8)))
            {
                JsonNode element;

                // while the file still has data to read
                while ((element = MAPPER.readTree(parser)) != null)
                {
                    InputLine input = parseInput(element);
                    if (!input.text().isBlank())
                    {
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

    // Returns the next input lines in the input queue
    public LineStorage getInputLines(int numberOfLines) throws IOException
    {
        LineStorage storage = new LineStorage();

        try
        {
            for (int i = 0; i < numberOfLines; i++)
            {
                InputLine next = queue.take();

                // if the end of file was reached, the put END_OF_FILE back into the queue so
                // other threads can read it.
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

    // Returns an InputLine containing the results from parsing a json element
    // If json element does not have url attribute, the InputLine will only contain
    // the text.
    private static InputLine parseInput(JsonNode element) throws IOException
    {
        // check if json node is text element
        if (!element.isObject() || !element.path("text").isTextual())
        {
            throw new IOException("Each input record must contain a string text field.");
        }
        JsonNode url = element.get("url");

        // if no url is included in the json file, just return an InputLine with no url
        if (url == null || url.isNull())
        {
            return new InputLine(element.get("text").asText(), null);
        }

        // if there is a "url" json element, make sure it is textual before using it
        if (!url.isTextual())
        {
            throw new IOException("The url field must be a string.");
        }
        try
        {
            URI uri = URI.create(url.asText());
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())))
            {
                throw new IOException("The url field must use http or https scheme.");
            }
            // return element InputLine element containing both url and text
            return new InputLine(element.get("text").asText(), uri);
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("Invalid webpage URL: " + url.asText(), e);
        }
    }

    // simple data structure storing a url/string pair
    private record InputLine(String text, URI uri)
    {
    }

    // Closes the file once reading is complete
    @Override
    public void close() throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            readerThread.interrupt();
        }
    }
}