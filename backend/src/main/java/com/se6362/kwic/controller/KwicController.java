package com.se6362.kwic.controller;

import com.se6362.kwic.service.KwicService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kwic")
public class KwicController {
    private final KwicService service;

    public KwicController(KwicService service) {
        this.service = service;
    }

    @PostMapping
    public KwicService.Result generate(@RequestBody Request request) {
        if (!(request.text() instanceof String text)) {
            throw new IllegalArgumentException("The text field must contain a string.");
        }
        return service.generate(text);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Error> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new Error(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> invalidJson() {
        return ResponseEntity.badRequest().body(new Error("Send a JSON object with a text field containing a string."));
    }

    // Accept the JSON value without scalar coercion, then require a string above.
    public record Request(Object text) {}
    public record Error(String message) {}
}
