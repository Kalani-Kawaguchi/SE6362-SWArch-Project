package com.se6362.kwic.core;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MasterControl
{
    private final OutputBuffer outputBuffer = new OutputBuffer();
    private final StringBuffer circularShifts = new StringBuffer();
    private final StringBuffer alphabetized = new StringBuffer();

    public void runIncremental(Path inputFile) throws IOException
    {
        int threadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Instantiate the shared input component
        try (InputBuffer inputBuffer = new InputBuffer(inputFile))
        {

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
                            LineStorage storage = inputBuffer.getInputLine();

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
        outputBuffer.saveToDB();
    }

    public String getOutput()

    {
        return outputBuffer.toString();
    }

    public String getCircularShifts()
    {
        return circularShifts.toString();
    }

    public String getAlphabetized()
    {
        return alphabetized.toString();
    }

    private void processLines(LineList lineList)
    {
        CircularShift circularShift = new CircularShift();
        circularShift.processLines(lineList);
        circularShifts.append(circularShift.toString());
        Alphabetizer alphabetizer = new Alphabetizer();
        alphabetizer.processLines(circularShift);
        alphabetized.append(alphabetizer.toString());
        outputBuffer.processLines(alphabetizer);
    }
}
