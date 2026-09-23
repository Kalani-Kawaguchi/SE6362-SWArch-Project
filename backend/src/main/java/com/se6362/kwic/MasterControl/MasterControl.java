package com.se6362.kwic.MasterControl;

import com.se6362.kwic.LineList.*;
import com.se6362.kwic.InputManager.*;
import com.se6362.kwic.OutputManager.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MasterControl
{
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
