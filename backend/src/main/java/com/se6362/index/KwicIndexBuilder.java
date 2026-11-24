package com.se6362.index;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;

import com.se6362.web.*;

public class KwicIndexBuilder
{
    private static final int BATCH_SIZE = 10;

    private PageList inputWebpages = new PageList();
    private PageList KwicIndex = new PageList();
    private Set<Character> filteredSymbols = new HashSet<>();
    private boolean filteredSymbolsConfigured = false;

    public PageList getKwicIndex()
    {
        return KwicIndex;
    }

    public PageList getInputWebpages()
    {
        return inputWebpages;
    }

    // Runs the KWIC indexing using incremental processing.
    public void buildIndex(InputStream inputStream) throws IOException
    {
        filteredSymbols.clear();
        filteredSymbolsConfigured = false;

        try (JsonParser parser = new JsonFactory().createParser(inputStream))
        {
            // Read the root JSON object.
            if (parser.nextToken() != JsonToken.START_OBJECT)
            {
                throw new IOException("Expected a JSON object.");
            }

            boolean webpageDataFound = false;

            while (parser.nextToken() != JsonToken.END_OBJECT)
            {
                String fieldName = parser.currentName();
                JsonToken token = parser.nextToken();

                if ("filteredSymbols".equals(fieldName))
                {
                    filteredSymbols.clear();
                    filteredSymbolsConfigured = true;

                    if (token == JsonToken.START_ARRAY)
                    {
                        while (parser.nextToken() != JsonToken.END_ARRAY)
                        {
                            if (parser.currentToken() != JsonToken.VALUE_STRING)
                            {
                                throw new IOException("Filtered symbols must be strings.");
                            }

                            String symbol = parser.getText();

                            if (symbol.length() != 1)
                            {
                                throw new IOException(
                                        "Each filtered symbol must contain exactly one character.");
                            }

                            filteredSymbols.add(symbol.charAt(0));
                        }
                    }
                    else if (token != JsonToken.VALUE_NULL)
                    {
                        throw new IOException("filteredSymbols must be a JSON array.");
                    }
                }
                else if ("webpageData".equals(fieldName))
                {
                    if (token != JsonToken.START_ARRAY)
                    {
                        throw new IOException("webpageData must be a JSON array.");
                    }

                    // Use default filtering only if no configuration was provided.
                    if (!filteredSymbolsConfigured)
                    {
                        setDefaultFilteredSymbols();
                    }

                    webpageDataFound = true;

                    processInput(parser);

                    break;
                }
                else
                {
                    parser.skipChildren();
                }
            }

            if (!webpageDataFound)
            {
                throw new IOException("Missing webpageData in JSON request.");
            }
        }
    }

    // Adds all Unicode punctuation and symbol characters to the filter.
    private void setDefaultFilteredSymbols()
    {
        filteredSymbols.clear();

        for (int i = 0; i <= Character.MAX_VALUE; i++)
        {
            char character = (char) i;
            int type = Character.getType(character);

            if (type == Character.CONNECTOR_PUNCTUATION ||
                    type == Character.DASH_PUNCTUATION ||
                    type == Character.START_PUNCTUATION ||
                    type == Character.END_PUNCTUATION ||
                    type == Character.INITIAL_QUOTE_PUNCTUATION ||
                    type == Character.FINAL_QUOTE_PUNCTUATION ||
                    type == Character.OTHER_PUNCTUATION ||
                    type == Character.MATH_SYMBOL ||
                    type == Character.CURRENCY_SYMBOL ||
                    type == Character.MODIFIER_SYMBOL ||
                    type == Character.OTHER_SYMBOL)
            {
                filteredSymbols.add(character);
            }
        }

    }

    // Runs concurrent processing on the webpage JSON array.
    private void processInput(JsonParser parser) throws IOException
    {
        int consumerThreadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(consumerThreadCount);

        try (InputBuffer inputBuffer = new InputBuffer(parser, consumerThreadCount))
        {
            for (int i = 0; i < consumerThreadCount; i++)
            {
                final int workerId = i;

                executor.submit(() ->
                {
                    int count = 1;

                    try
                    {
                        while (true)
                        {
                            PageList input = inputBuffer.getNextBatch(BATCH_SIZE);

                            if (input.getPageCount() == 0)
                            {
                                break;
                            }

                            this.processLines(input);

                            System.out.println(
                                    "Worker " + workerId + " processed batch " + count + ".");

                            count++;

                            if (input.getPageCount() < BATCH_SIZE)
                            {
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        System.err.println(
                                "Worker " + workerId + " crashed: " + e.getMessage());
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        }
        catch (InterruptedException e)
        {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IOException("Index processing was interrupted.", e);
        }
    }

    private void processLines(PageList inputPages)
    {
        updateInput(inputPages);

        KwicProcessingStep symbolFilter = new SymbolFilter(inputPages, filteredSymbols);

        PageList filteredPages = symbolFilter.getOutput();

        KwicProcessingStep circularShift = new CircularShifter(filteredPages);

        PageList shiftedPages = circularShift.getOutput();

        KwicProcessingStep noiseWorkRemover = new NoiseWordRemover(shiftedPages);

        PageList noiseWordlessPages = noiseWorkRemover.getOutput();

        KwicProcessingStep alphabetizer = new Alphabetizer(noiseWordlessPages);

        PageList alphabetizedPages = alphabetizer.getOutput();

        updateOutput(alphabetizedPages);
    }

    private synchronized void updateOutput(PageList newOutput)
    {
        KwicProcessingStep merger = new Merger(KwicIndex, newOutput);
        this.KwicIndex = merger.getOutput();
    }

    private synchronized void updateInput(PageList newInput)
    {
        inputWebpages.append(newInput);
    }
}