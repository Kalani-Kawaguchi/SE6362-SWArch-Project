package com.se6362.kwic.MasterControl;

import com.se6362.kwic.LineList.*;
import com.se6362.kwic.InputManager.*;
import com.se6362.kwic.OutputManager.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class MasterControl
{
    /** Runs the same KWIC pipeline for an in-memory request without shared output state. */
    public static List<String> processLines(List<String> lines)
    {
        StorageLineList input = new StorageLineList();
        for (String line : lines)
        {
            input.addLine(line);
        }
        AlphabetizedLineList sorted = processBatch(input);
        List<String> result = new ArrayList<>();
        for (int lineNo = 0; lineNo < sorted.getLineCount(); lineNo++)
        {
            List<String> words = new ArrayList<>();
            for (int wordNo = 0; wordNo < sorted.getWordCount(lineNo); wordNo++)
            {
                words.add(sorted.getWord(lineNo, wordNo));
            }
            result.add(String.join(" ", words));
        }
        return List.copyOf(result);
    }

    public static void runKwicSystem(String filePath, int batchSize)
    {
        int threadCount = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        OutputManager outputManager = new OutputManager();

        // Instantiate the shared input component
        try (InputManager inputManager = new InputManager(filePath))
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
                            StorageLineList lineList = inputManager.getNextBatch(batchSize);

                            // Break out if the inputManager file component reports empty (EOF)
                            if (lineList.getLineCount() == 0)
                            {
                                break;
                            }

                            outputManager.writeToBuffer(processBatch(lineList));
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
        outputManager.outputBuffer();
    }

    private static AlphabetizedLineList processBatch(LineList lineList)
    {
        CircShiftedLineList circularShift = new CircShiftedLineList(lineList);
        return new AlphabetizedLineList(circularShift);
    }
}
