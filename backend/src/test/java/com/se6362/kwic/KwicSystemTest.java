package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.se6362.kwic.core.*;

import java.io.IOException;
import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
public class KwicSystemTest
{

    @Test
    void testKwicSystem() throws IOException
    {
        MasterControl masterControl = new MasterControl();
        masterControl.runIncremental(Path.of("testData.jsonl"));
        String output = masterControl.getOutput();
        String predictedOutput = "A search engine helps users find relevant information quickly \n" +
                "brown fox jumps over the lazy dog The quick \n" +
                "designing reliable software systems Software engineering is the process of \n" +
                "dog The quick brown fox jumps over the lazy \n" +
                "engine helps users find relevant information quickly A search \n" +
                "engineering is the process of designing reliable software systems Software \n" +
                "find relevant information quickly A search engine helps users \n" +
                "fox jumps over the lazy dog The quick brown \n" +
                "helps users find relevant information quickly A search engine \n" +
                "information quickly A search engine helps users find relevant \n" +
                "is the process of designing reliable software systems Software engineering \n" +
                "jumps over the lazy dog The quick brown fox \n" +
                "lazy dog The quick brown fox jumps over the \n" +
                "of designing reliable software systems Software engineering is the process \n" +
                "over the lazy dog The quick brown fox jumps \n" +
                "process of designing reliable software systems Software engineering is the \n" +
                "quick brown fox jumps over the lazy dog The \n" +
                "quickly A search engine helps users find relevant information \n" +
                "relevant information quickly A search engine helps users find \n" +
                "reliable software systems Software engineering is the process of designing \n" +
                "search engine helps users find relevant information quickly A \n" +
                "Software engineering is the process of designing reliable software systems \n" +
                "software systems Software engineering is the process of designing reliable \n" +
                "systems Software engineering is the process of designing reliable software \n" +
                "the lazy dog The quick brown fox jumps over \n" +
                "the process of designing reliable software systems Software engineering is \n" +
                "The quick brown fox jumps over the lazy dog \n" +
                "users find relevant information quickly A search engine helps \n";
        assertEquals(predictedOutput, output);
    }
}