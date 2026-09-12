package com.se6362.kwic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController 
@RequestMapping ("/api")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "KWIC backend is running!";
    }
}