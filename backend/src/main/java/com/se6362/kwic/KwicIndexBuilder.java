/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: MasterControl.java
    Description: Controls sequence of KWIC processing steps all the way from input to output
 */

package com.se6362.kwic;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.se6362.database.DatabaseManager;
import com.se6362.webpage.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.InputStream;

public class KwicIndexBuilder
{
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 1;
    private PageList input = new PageList();
    private PageList output = new PageList();
    ObjectNode jsonOutput;
    ArrayNode incrementalOutputArray;

    public KwicIndexBuilder()
    {
        jsonOutput = JSON_MAPPER.createObjectNode();
        incrementalOutputArray = jsonOutput.putArray("incrementalOutput");
    }

    // Runs the KWIC indexing system with "inputFile" as input. Uses incremental
    // processing
    public void buildIndex(InputStream inputStream) throws IOException
    {
        incrementalOutputArray.removeAll();
        int consumerThreadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(consumerThreadCount);

        // Instantiate the shared input component
        try (InputBuffer inputBuffer = new InputBuffer(inputStream, consumerThreadCount))
        {

            // Launch Multiple Threads to process the input lines
            for (int i = 0; i < consumerThreadCount; i++)
            {
                final int workerId = i;
                executor.submit(() ->
                {
                    try
                    {
                        // Continue to read and process lines until all lines have been processed
                        while (true)
                        {
                            // Consumers pull work directly from the independent component
                            PageList input = inputBuffer.getNextBatch(BATCH_SIZE);

                            // Break out if unable to read max number of pages, indicating end of input
                            // stream

                            this.processLines(input);

                            if (input.getPageCount() < BATCH_SIZE)
                            {
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        System.err.println("Worker " + workerId + " crashed: " + e.getMessage());
                    }
                });
            }

            // Await execution completion
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Saves output stored in output buffer to a SQL database
    public void saveIndex(DatabaseManager database)
    {
        database.insertWebpages(input);
        database.insertIndexEntries(output);
    }

    // Returns all output lines. Used to display output back to user.
    public String getOutput()
    {
        return output.toString();
    }

    // Returns a json representation of both incremental and merged output
    public synchronized ObjectNode toJsonElement()
    {
        jsonOutput.put("mergedOutput", getOutput());
        return jsonOutput;
    }

    // Processes Lines in a LineList object by perfroming the circular shifting,
    // alphabetization, and merging
    // This is where must of the work happens to build the KWIC index.
    private void processLines(PageList inputPages)
    {
        updateInput(inputPages);
        CircularShifter circularShift = new CircularShifter(inputPages);
        PageList shiftedPages = circularShift.getOutput();
        Alphabetizer alphabetizer = new Alphabetizer(shiftedPages);
        PageList alphabetizedPages = alphabetizer.getOutput();
        updateOutput(alphabetizedPages);

        ObjectNode incrementalOutput = JSON_MAPPER.createObjectNode();
        incrementalOutput.put("inputLines", inputPages.toString());
        incrementalOutput.put("shiftedLines", shiftedPages.toString());
        incrementalOutput.put("alphabetizedLines", alphabetizedPages.toString());
        appendIncrementalJson(incrementalOutput);
    }

    private synchronized void updateOutput(PageList newOutput)
    {
        Merger merger = new Merger(output, newOutput);
        this.output = merger.getOutput();
    }

    private synchronized void updateInput(PageList newInput)
    {
        input.append(newInput);
    }

    // Appends a new json element to the existing json array of incremental output
    private synchronized void appendIncrementalJson(ObjectNode incrementalOutput)
    {
        incrementalOutputArray.add(incrementalOutput);
    }
}
