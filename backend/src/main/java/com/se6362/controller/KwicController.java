/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: KwicController.java
    Description: REST controller mapping POST requests sent from the KWIC Index Builder
 */

package com.se6362.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.se6362.database.DatabaseManager;
import com.se6362.kwic.InputBuffer;
import com.se6362.kwic.JsonReader;
import com.se6362.kwic.KwicIndexBuilder;
import com.se6362.webpage.*;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;

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

    // Generate a KWIC index from user input and return the result as json file
    @PostMapping(value = "/kwic/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized String addWebpages(HttpServletRequest request) throws IOException
    {
        System.out.println("KWIC Input Received");

        KwicIndexBuilder indexBuilder = new KwicIndexBuilder();

        indexBuilder.buildIndex(request.getInputStream());
        indexBuilder.saveIndex(database);

        ObjectNode json = indexBuilder.toJsonElement();
        String sortedShifts = indexBuilder.getOutput();

        System.out.println("KWIC JSON:\n" + json.toPrettyString());
        System.out.println("KWIC sorted output:\n" + sortedShifts);

        return json.toString();
    }

    @PostMapping(value = "/kwic/sponsor", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String sponsorWebpages(@RequestBody JsonNode json) throws IOException
    {
        if (json == null || !json.isArray() || json.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-empty JSON array required");
        }

        for (JsonNode element : json)
        {
            if (!element.isObject() || element.get("sponsorship_level") == null)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webpage object");
            }

            WebPage page = JsonReader.parseJsonWebpage(element);
            int sponsorshipLevel = element.get("sponsorship_level").asInt();

            database.addSponsorship(page, sponsorshipLevel);
        }

        System.out.println("Sponsorships addeed.");
        return "Successful";
    }

    // Delete current user generated KWIC index
    @DeleteMapping(value = "/kwic/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized String deleteWebpage(@RequestBody JsonNode json) throws IOException
    {
        if (json == null || !json.isArray() || json.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-empty JSON array required");
        }

        PageList pagesToDelete = new PageList();

        for (JsonNode element : json)
        {
            if (!element.isObject())
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webpage object");
            }

            WebPage page = JsonReader.parseJsonWebpage(element);

            pagesToDelete.append(page);
        }

        System.out.println("Webpages deleted");
        return "Successful";
    }
}
