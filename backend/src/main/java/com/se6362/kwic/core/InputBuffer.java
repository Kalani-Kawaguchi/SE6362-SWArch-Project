/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: InputBuffer.java
    Description: Reads JSON file containing lines of text and temporarily stores lines prior to KWIC index generation.
 */

package com.se6362.kwic.core;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InputBuffer implements AutoCloseable
{
    private final BlockingQueue<WebPage> buffer = new LinkedBlockingQueue<>(1000);

    private Thread readerThread;
    private volatile IOException readException;

    // Reads in data from file in a separate thread. Use getInputLines to get a line
    // of input that was read.
    public void readIntoBuffer(InputStream inputStream) throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            throw new IOException("A file is already being read.");
        }

        buffer.clear();
        readException = null;
        JsonReader reader = new JsonReader(inputStream, buffer);

        // Creates new thread to read file.
        readerThread = new Thread(() ->
        {
            try
            {
                reader.readFile();
            }
            catch (IOException exception)
            {
                readException = exception;
            }
        });

        readerThread.start();
    }

    // Returns the next input lines in the input queue
    public synchronized PageList getInput(int numberOfLines) throws IOException
    {
        PageList pages = new PageList();

        try
        {
            for (int i = 0; i < numberOfLines; i++)
            {
                WebPage next = buffer.take();

                // if the end of file was reached, the put END_OF_FILE back into the queue so
                // other threads can read it.
                if (next == JsonReader.END_OF_FILE)
                {
                    buffer.put(JsonReader.END_OF_FILE);

                    if (readException != null)
                    {
                        throw readException;
                    }
                    break;
                }

                // Add the line(s) from next to storage.
                pages.appendPage(next);
            }

            return pages;
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new IOException("Thread interrupted while waiting for input.", exception);
        }
    }

    // closes the file once reading is complete
    @Override
    public void close() throws IOException
    {
        if (readerThread != null && readerThread.isAlive())
        {
            readerThread.interrupt();
        }
    }
}