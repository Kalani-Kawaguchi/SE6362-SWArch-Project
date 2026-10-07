package com.se6362.kwic.core;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InputBuffer implements AutoCloseable
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String END_OF_FILE = "";

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

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
                    new FileReader(filePath.toString())))
            {
                JsonNode element;

                while ((element = MAPPER.readTree(parser)) != null)
                {
                    queue.put(element.path("text").asText());
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
                String next = queue.take();

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
                storage.storeLine(next);
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

    @Override
    public void close() throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            readerThread.interrupt();
        }
    }
}