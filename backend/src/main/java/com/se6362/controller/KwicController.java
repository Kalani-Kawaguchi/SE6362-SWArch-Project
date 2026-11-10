/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: KwicController.java
    Description: REST controller mapping POST requests sent from the KWIC Index Builder
 */

package com.se6362.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.se6362.database.DatabaseManager;
import com.se6362.kwic.JsonReader;
import com.se6362.kwic.KwicIndexBuilder;
import com.se6362.webpage.*;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;

@CrossOrigin(origins = "http://localhost:1234")
@RestController
@RequestMapping("/api/kwic")
public class KwicController
{
    private final DatabaseManager database;
    private final ObjectMapper objectMapper;

    public KwicController(DatabaseManager database)
    {
        this.database = database;
        this.objectMapper = new ObjectMapper();
    }

    // Generate a KWIC index from user input and return the result as json file
    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized String addWebpages(HttpServletRequest request) throws IOException
    {
        System.out.println("KWIC Input Received");

        KwicIndexBuilder indexBuilder = new KwicIndexBuilder();

        indexBuilder.buildIndex(request.getInputStream());
        System.out.println("Index has been built.");
        indexBuilder.saveIndex(database);

        return "successful";
    }

    @PostMapping(value = "/sponsor", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String sponsorWebpages(@RequestBody List<Map<String, Object>> payload) throws IOException
    {
        if (payload == null || payload.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-empty JSON array required");
        }

        for (Map<String, Object> element : payload)
        {
            if (element == null || element.get("sponsorship_level") == null
                    || element.get("title") == null
                    || element.get("url") == null
                    || element.get("description") == null)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webpage object");
            }

            JsonNode json = objectMapper.valueToTree(element);

            WebPage page = JsonReader.parseJson(json);
            int sponsorshipLevel = ((Number) element.get("sponsorship_level")).intValue();

            database.addSponsorship(page, sponsorshipLevel);
        }

        System.out.println("Sponsorships added.");
        return "Successful";
    }

    // Delete current user generated KWIC index
    @DeleteMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized String deleteWebpage(@RequestBody List<Map<String, Object>> payload) throws IOException
    {
        if (payload == null || payload.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-empty JSON array required");
        }

        PageList pagesToDelete = new PageList();

        for (Map<String, Object> element : payload)
        {
            if (element == null
                    || element.get("title") == null
                    || element.get("url") == null
                    || element.get("description") == null)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webpage object");
            }

            JsonNode json = objectMapper.valueToTree(element);

            WebPage page = JsonReader.parseJson(json);

            pagesToDelete.append(page);
        }

        database.deleteWebpages(pagesToDelete);

        System.out.println("Webpages deleted");
        return "Successful";
    }
}
