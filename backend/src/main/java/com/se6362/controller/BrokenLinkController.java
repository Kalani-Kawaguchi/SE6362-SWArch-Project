package com.se6362.controller;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.se6362.service.BrokenLinkService;
import com.se6362.service.BrokenLinkService.DeleteResult;
import com.se6362.service.BrokenLinkService.LinkResult;

@RestController
@RequestMapping("/api/kwic/broken-links")
public class BrokenLinkController {
    private final BrokenLinkService service;
    private final Semaphore operation = new Semaphore(1);

    public BrokenLinkController(BrokenLinkService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public List<LinkResult> scan() {
        acquire();

        try {
            return service.scan();
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Scan interrupted");
        }
        finally {
            operation.release();
        }
    }

    @PostMapping("/delete")
    public DeleteResult delete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()
                || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid page ID required");
        }

        acquire();

        try {
            return service.delete(ids);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Delete interrupted");
        }
        finally {
            operation.release();
        }
    }

    private void acquire() {
        if (!operation.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Operation in progress");
        }
    }
}
