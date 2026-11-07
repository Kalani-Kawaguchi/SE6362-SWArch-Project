/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: KwicController.java
    Description: REST controller mapping POST requests sent from the KWIC Index Builder
 */

package com.se6362.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.se6362.core.*;
import com.se6362.database.DatabaseManager;
import com.se6362.kwic.KwicIndexBuilder;

@CrossOrigin(origins = "http://localhost:1234")
@RestController
@RequestMapping("/api")
public class KwicController
{
    private final DatabaseManager database;

    public KwicController(DatabaseManager database)
    {
        this.database = database;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Generate a KWIC index from user input and return the result as json file
    @PostMapping(value = "/kwic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public synchronized String addWebpages(@RequestParam("file") MultipartFile file) throws IOException
    {

        if (file.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File required");
        }

        System.out.println("KWIC Input Received: " + file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream())
        {
            KwicIndexBuilder indexBuilder = new KwicIndexBuilder();
            indexBuilder.buildIndex(inputStream);
            indexBuilder.saveIndex(database);

            ObjectNode json = indexBuilder.toJsonElement();
            String sortedShifts = indexBuilder.getOutput();

            System.out.println("KWIC JSON:\n" + json.toPrettyString());
            System.out.println("KWIC sorted output:\n" + sortedShifts);

            return json.toString();
        }
    }

    // Delete current user generated KWIC index
    @DeleteMapping("/kwic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public synchronized void clear()
    {
        // TO DO: delete database entries
    }
}
