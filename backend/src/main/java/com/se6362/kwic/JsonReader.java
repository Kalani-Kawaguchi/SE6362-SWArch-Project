package com.se6362.kwic;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.se6362.core.*;

public class JsonReader implements AutoCloseable
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonParser parser;
    private JsonNode nextElement;
    private boolean arrayFinished = false;

    public JsonReader(InputStream inputStream) throws IOException
    {
        parser = MAPPER.createParser(inputStream);

        // Verify that the input starts with a JSON array.
        if (parser.nextToken() != JsonToken.START_ARRAY)
        {
            throw new IOException("JSON input must contain an array.");
        }

    }

    public boolean hasNext() throws IOException
    {
        if (nextElement == null && !arrayFinished)
        {
            JsonToken token = parser.nextToken();

            if (token == JsonToken.END_ARRAY || token == null)
            {
                arrayFinished = true;
            }
            else if (token == JsonToken.START_OBJECT)
            {
                nextElement = MAPPER.readTree(parser);
            }
            else
            {
                throw new IOException("Each JSON array element must be an object.");
            }
        }

        return nextElement != null;
    }

    public WebPage readNext() throws IOException
    {
        if (!hasNext())
        {
            return null;
        }

        JsonNode element = nextElement;
        nextElement = null;

        return parseJsonWebpage(element);
    }

    public static WebPage parseJsonWebpage(JsonNode element) throws IOException
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

    @Override
    public void close() throws IOException
    {
        parser.close();
    }
}