package com.se6362.kwic.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class MasterControl
{
    private OutputBuffer completed;

    public List<String> runIncremental(Path inputFile) throws IOException
    {
        completed = null;

        LineStorage lineStorage = new LineStorage();
        InputBuffer inputBuffer = new InputBuffer(lineStorage);
        CircularShift circularShift = new CircularShift(lineStorage);
        Alphabetizer alphabetizer = new Alphabetizer(circularShift);
        OutputBuffer outputBuffer = new OutputBuffer(alphabetizer);

        // Create a reader for the input file
        try (BufferedReader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8))
        {
            // Process input line by line
            while (inputBuffer.getInputLine(reader)) {
                circularShift.readLines();
                alphabetizer.readLines();
                outputBuffer.readLines();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        completed = outputBuffer;
        return completed.getLines();
    }

    public String saveOutput() throws SQLException {
        return "";
    }
}
