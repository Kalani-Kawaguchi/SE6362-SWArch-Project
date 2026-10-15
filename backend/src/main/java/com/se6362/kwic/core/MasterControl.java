/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: MasterControl.java
    Description: Controls sequence of KWIC processing steps all the way from input to output
 */

package com.se6362.kwic.core;

import java.io.IOException;
import java.nio.file.Path;
import com.se6362.kwic.database.IndexDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class MasterControl
{
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private final OutputBuffer outputBuffer = new OutputBuffer();
    ObjectNode jsonOutput;
    ArrayNode incrementalOutputArray;

    public MasterControl()
    {
        jsonOutput = JSON_MAPPER.createObjectNode();
        incrementalOutputArray = jsonOutput.putArray("incrementalOutput");
    }

    // Runs the KWIC indexing system with "inputFile" as input. Uses incremental
    // processing
    public void runIncremental(Path inputFile) throws IOException
    {
        incrementalOutputArray.removeAll();
        int threadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Instantiate the shared input component
        try (InputBuffer inputBuffer = new InputBuffer())
        {
            inputBuffer.readFromFile(inputFile);

            // Launch Multiple Threads to process the input lines
            for (int i = 0; i < threadCount; i++)
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
                            LineStorage storage = inputBuffer.getInputLines(1);

                            // Break out if the inputManager file component reports
                            if (storage.getLineCount() == 0)
                            {
                                break;
                            }

                            this.processLines(storage);
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
    public int saveOutput(IndexDatabase database)
    {
        return outputBuffer.writeToDatabase(database);
    }

    // Returns all output lines. Used to display output back to user.
    public String getOutput()
    {
        return outputBuffer.toString();
    }

    // Returns a LineList object containing the output lines.
    public LineStorage getOutputLines()
    {
        return outputBuffer.getOutputLines();
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
    private void processLines(LineList lineList)
    {
        CircularShift circularShift = new CircularShift();
        circularShift.circularShiftLines(lineList);
        Alphabetizer alphabetizer = new Alphabetizer();
        alphabetizer.alphabetizeLines(circularShift);
        outputBuffer.setOutputLines(alphabetizer);

        ObjectNode incrementalOutput = JSON_MAPPER.createObjectNode();
        incrementalOutput.put("inputLines", lineList.toString());
        incrementalOutput.put("shiftedLines", circularShift.toString());
        incrementalOutput.put("alphabetizedLines", alphabetizer.toString());
        appendIncrementalJson(incrementalOutput);
    }

    // Appends a new json element to the existing json array of incremental output
    private synchronized void appendIncrementalJson(ObjectNode incrementalOutput)
    {
        incrementalOutputArray.add(incrementalOutput);
    }

    // clears the current output in the buffer (does not clear the database entries)
    public synchronized void clear()
    {
        outputBuffer.clear();
        incrementalOutputArray.removeAll();
        jsonOutput.put("output", "");
    }
}
