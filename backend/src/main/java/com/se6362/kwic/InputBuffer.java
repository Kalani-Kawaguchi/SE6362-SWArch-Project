package com.se6362.kwic;

import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.se6362.core.*;

public class InputBuffer implements AutoCloseable
{
    private final BlockingQueue<WebPage> buffer = new LinkedBlockingQueue<>(1000);

    private static final WebPage END_OF_FILE = new WebPage("", "", "END_OF_FILE");

    private final Thread readerThread;
    private volatile IOException readException = null;

    public InputBuffer(InputStream inputStream, int consumerCount) throws IOException
    {
        readerThread = new Thread(() ->
        {
            try (JsonReader reader = new JsonReader(inputStream))
            {
                while (reader.hasNext())
                {
                    buffer.put(reader.readNext());
                }
            }
            catch (IOException e)
            {
                readException = e;
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
            finally
            {
                // One poison pill for each consumer.
                for (int i = 0; i < consumerCount; i++)
                {
                    try
                    {
                        buffer.put(END_OF_FILE);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        readerThread.start();
    }

    public PageList getNextBatch(int batchSize) throws IOException
    {
        PageList pages = new PageList();

        try
        {
            for (int i = 0; i < batchSize; i++)
            {
                WebPage next = buffer.take();

                if (next == END_OF_FILE)
                {
                    if (readException != null)
                    {
                        throw readException;
                    }

                    break;
                }

                pages.appendPage(next);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("Input processing interrupted.", e);
        }

        return pages;
    }

    @Override
    public void close() throws IOException
    {
        readerThread.interrupt();

        try
        {
            readerThread.join();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while closing InputBuffer.", e);
        }
    }
}