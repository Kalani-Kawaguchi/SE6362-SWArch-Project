package com.se6362.kwic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class BrokenLinkService
{
    public record LinkResult(String url, String status, String reason)
    {
    }

    private final Semaphore concurrencyLimiter = new Semaphore(25);

    // Per-scan or short-lived DNS safety cache to avoid repeated lookups for the
    // same host
    private final Map<String, Boolean> hostSafetyCache = new ConcurrentHashMap<>();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public List<LinkResult> scanUrls(List<String> urls) throws InterruptedException
    {
        if (urls.isEmpty())
        {
            return List.of();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor())
        {
            List<Callable<StatusOutcome>> tasks = urls.stream().<Callable<StatusOutcome>>map(url -> () ->
            {
                concurrencyLimiter.acquire();
                try
                {
                    return checkUrl(url);
                }
                finally
                {
                    concurrencyLimiter.release();
                }
            }).toList();

            var futures = executor.invokeAll(tasks, 45, TimeUnit.SECONDS);

            List<LinkResult> results = new java.util.ArrayList<>();

            for (int i = 0; i < futures.size(); i++)
            {
                var future = futures.get(i);
                String url = urls.get(i);

                if (future.isCancelled())
                {
                    results.add(new LinkResult(
                            url, "UNKNOWN", "Scan time limit reached; kept"));
                    continue;
                }

                try
                {
                    StatusOutcome outcome = future.get();

                    results.add(new LinkResult(
                            url, outcome.status(), outcome.reason()));
                }
                catch (ExecutionException exception)
                {
                    throw new IllegalStateException(
                            "Unable to complete the link scan",
                            exception.getCause());
                }
            }

            return results;
        }
    }

    private StatusOutcome checkUrl(String rawUrl) throws InterruptedException
    {
        try
        {
            URI uri = URI.create(rawUrl);

            if (!isPublicWebAddress(uri))
            {
                return new StatusOutcome("UNKNOWN", "Unsupported or non-public URL");
            }

            // 4. Try lightweight HEAD first; only fall back to GET if HEAD is unsupported
            int status = sendProbe(uri, "HEAD");
            if (status == 405 || status == 501)
            {
                status = sendProbe(uri, "GET");
            }

            if (status == 404 || status == 410)
            {
                return new StatusOutcome("BROKEN", "HTTP " + status);
            }
            if (status >= 200 && status < 300)
            {
                return new StatusOutcome("WORKING", "HTTP " + status);
            }

            return new StatusOutcome("UNKNOWN", "HTTP " + status + "; kept");
        }
        catch (IOException | IllegalArgumentException exception)
        {
            return new StatusOutcome("UNKNOWN", "Unable to verify; kept");
        }
    }

    private int sendProbe(URI uri, String method) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "KWIC-LinkChecker/1.0")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = client.send(
                request,
                HttpResponse.BodyHandlers.discarding());

        return response.statusCode();
    }

    private boolean isPublicWebAddress(URI uri) throws IOException
    {
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
        {
            return false;
        }

        if (uri.getHost() == null || uri.getUserInfo() != null)
        {
            return false;
        }

        int expectedPort = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        if (uri.getPort() != -1 && uri.getPort() != expectedPort)
        {
            return false;
        }

        String host = uri.getHost().toLowerCase();
        Boolean cached = hostSafetyCache.get(host);
        if (cached != null)
        {
            return cached;
        }

        boolean isPublic = resolveAndValidateHost(host);
        hostSafetyCache.put(host, isPublic);
        return isPublic;
    }

    private boolean resolveAndValidateHost(String host) throws IOException
    {
        for (InetAddress address : InetAddress.getAllByName(host))
        {
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
                    || sharedIpv4)
            {
                return false;
            }
        }
        return true;
    }

    private record StatusOutcome(String status, String reason)
    {
    }
}
