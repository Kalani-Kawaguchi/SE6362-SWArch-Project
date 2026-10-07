package com.se6362.kwic.core;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
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

    public void runIncremental(Path inputFile) throws IOException
    {
        incrementalOutputArray.removeAll();
        int threadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Instantiate the shared input component
        try (InputBuffer inputBuffer = new InputBuffer())
        {
            inputBuffer.readFromFile(inputFile);

            // Launch consumers
            for (int i = 0; i < threadCount; i++)
            {
                final int workerId = i;
                executor.submit(() ->
                {
                    try
                    {
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

    public void saveOutput() throws SQLException
    {
        outputBuffer.writeToDatabase();
    }

    public String getOutput()
    {
        return outputBuffer.toString();
    }

    public synchronized ObjectNode toJsonElement()
    {
        jsonOutput.put("mergedOutput", getOutput());
        return jsonOutput;
    }

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

    private synchronized void appendIncrementalJson(ObjectNode incrementalOutput)
    {
        incrementalOutputArray.add(incrementalOutput);
    }

    public synchronized void clear()
    {
        outputBuffer.clear();
        incrementalOutputArray.removeAll();
        jsonOutput.put("output", "");
    }
}
