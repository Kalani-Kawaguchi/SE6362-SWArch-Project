package com.se6362.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.se6362.database.DatabaseManager;
import com.se6362.database.DatabaseManager.PageLink;

@Service 
public class BrokenLinkService {
    public record LinkResult(long id, String url, String status, String reason) {}

    public record DeleteResult(int deleted, int kept) {}

    private final DatabaseManager database;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public BrokenLinkService(DatabaseManager database) {
        this.database = database;
    }

    public List<LinkResult> scan() throws InterruptedException {
        return checkAll(database.getPageLinks());
    }

    public DeleteResult delete(List<Long> ids) throws InterruptedException {
        var requestedIds = new HashSet<>(ids);

        List<PageLink> candidates = database.getPageLinks().stream()
                .filter(page -> requestedIds.contains(page.id()))
                .toList();

        List<PageLink> confirmed = checkAll(candidates).stream()
                .filter(result -> result.status().equals("BROKEN"))
                .map(result -> new PageLink(result.id(), result.url()))
                .toList();

        int deleted = database.deleteBrokenLinks(confirmed);

        return new DeleteResult(deleted, candidates.size() - deleted);
    }

    private List<LinkResult> checkAll(List<PageLink> pages) throws InterruptedException {
        List<Callable<LinkResult>> tasks = pages.stream()
                .<Callable<LinkResult>>map(page -> () -> check(page))
                .toList();

        var executor = Executors.newFixedThreadPool(4);

        try {
            var futures = executor.invokeAll(tasks, 45, TimeUnit.SECONDS);
            List<LinkResult> results = new ArrayList<>();

            for (int i = 0; i < futures.size(); i++) {
                var future = futures.get(i);
                PageLink page = pages.get(i);

                if (future.isCancelled()) {
                    results.add(result(page, "UNKNOWN", "Scan time limit reached; kept"));
                    continue;
                }

                try {
                    results.add(future.get());
                }
                catch (ExecutionException exception) {
                    throw new IllegalStateException("Unable to complete the link scan", exception.getCause());
                }
            }

            return results;
        }
        finally {
            executor.shutdownNow();
        }
    }

    private LinkResult check(PageLink page) throws InterruptedException {
        try {
            URI uri = URI.create(page.url());

            if (!isPublicWebAddress(uri)) {
                return result(page, "UNKNOWN", "Unsupported or non-public URL");
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "KWIC-LinkChecker/1.0")
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(
                    request,
                    info -> HttpResponse.BodySubscribers.replacing(null));
            
            int status = response.statusCode();

            if (status == 404 || status == 410) {
                return result(page, "BROKEN", "HTTP " + status);
            }
            if (status >= 200 && status < 300) {
                return result(page, "WORKING", "HTTP " + status);
            }
            
            return result(page, "UNKNOWN", "HTTP " + status + "; kept");
        }
        catch (IOException | IllegalArgumentException exception) {
            return result(page, "UNKNOWN", "Unable to verify; kept");
        }
    }

    private boolean isPublicWebAddress(URI uri) throws IOException {
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        if (uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }

        int expectedPort = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;

        if (uri.getPort() != -1 && uri.getPort() != expectedPort) {
            return false;
        }

        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            byte[] bytes = address.getAddress();
            boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;

            boolean sharedIpv4 = bytes.length == 4
                    && (bytes[0] & 0xff) == 100
                    && (bytes[1] & 0xff) >= 64
                    && (bytes[1] & 0xff) <= 127;

            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()
                    || uniqueLocalIpv6
                    || sharedIpv4) {
                return false;
            }
        }
        return true;
    }

    private LinkResult result(PageLink page, String status, String reason) {
        return new LinkResult(page.id(), page.url(), status, reason);
    }
}
