package com.se6362.kwic;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.se6362.core.*;

public class JsonReader
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final WebPage END_OF_FILE = new WebPage("", "", "END_OF_FILE");

    private BlockingQueue<WebPage> queue;
    private InputStream inputStream;

    public JsonReader(InputStream inputStream, BlockingQueue<WebPage> queue)
    {
        this.inputStream = inputStream;
        this.queue = queue;
    }

    public void readFile() throws IOException
    {
        try (JsonParser parser = MAPPER.createParser(inputStream))
        {
            JsonNode element;

            while ((element = MAPPER.readTree(parser)) != null)
            {
                WebPage input = parseInput(element);

                queue.put(input);
            }
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            try
            {
                queue.put(JsonReader.END_OF_FILE);
            }
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static WebPage parseInput(JsonNode element) throws IOException
    {
        if (!element.isObject())
        {
            throw new IOException("Each JSON input record must contain a JSON object.");
        }

        String title = "";
        String url = "";
        String description = "";

        if (element.get("title") != null)
        {
            title = element.get("title").asText();
        }
        if (element.get("url") != null)
        {
            url = element.get("url").asText();
        }
        if (element.get("description") != null)
        {
            description = element.get("description").asText();
        }

        try
        {
            return new WebPage(title, url, description);
        }
        catch (IllegalArgumentException exception)
        {
            throw new IOException("Invalid webpage record.", exception);
        }
    }
}