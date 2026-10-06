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
    ArrayNode circularShiftArray;
    ArrayNode alphabetizerArray;

    public MasterControl()
    {
        jsonOutput = JSON_MAPPER.createObjectNode();
        circularShiftArray = jsonOutput.putArray("circularShift");
        alphabetizerArray = jsonOutput.putArray("alphabetizer");
    }

    public void runIncremental(Path inputFile) throws IOException
    {
        circularShiftArray.removeAll();
        alphabetizerArray.removeAll();
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
        jsonOutput.put("output", getOutput());
        return jsonOutput;
    }

    private void processLines(LineList lineList)
    {
        CircularShift circularShift = new CircularShift();
        circularShift.circularShiftLines(lineList);
        appendJson(circularShiftArray, circularShift.toString());
        Alphabetizer alphabetizer = new Alphabetizer();
        alphabetizer.alphabetizeLines(circularShift);
        appendJson(alphabetizerArray, alphabetizer.toString());
        outputBuffer.setOutputLines(alphabetizer);
    }

    private synchronized void appendJson(ArrayNode array, String lines)
    {
        array.add(lines);
    }
}
