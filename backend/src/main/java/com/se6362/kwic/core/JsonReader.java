package com.se6362.kwic.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonReader
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final WebPage END_OF_FILE = new WebPage("", "", "END_OF_FILE");
    private BlockingQueue<WebPage> queue;
    private Path filePath;

    public JsonReader(Path filePath, BlockingQueue<WebPage> queue)
    {
        this.filePath = filePath;
        this.queue = queue;
    }

    public void readFile() throws IOException
    {
        try (JsonParser parser = MAPPER.createParser(Files.newBufferedReader(filePath, StandardCharsets.UTF_8)))
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
            throw new IOException("Each JSON input record must contain a string text field.");
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
