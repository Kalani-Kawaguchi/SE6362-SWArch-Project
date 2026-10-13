package com.se6362.kwic.index;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class UrlFetcher
{
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_BYTES = 2_000_000;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    public boolean isAllowed(String url)
    {
        final URI uri;
        try
        {
            uri = new URI(url);
        }
        catch (URISyntaxException e)
        {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
        {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank())
        {
            return false;
        }

        try
        {
            for (InetAddress address : InetAddress.getAllByName(host))
            {
                if (address.isLoopbackAddress()
                        || address.isAnyLocalAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress())
                {
                    return false;
                }
            }
        }
        catch (UnknownHostException e)
        {
            return false;
        }

        return true;
    }

    /** Returns the response body as a string, or throws FetchException. */
    public String fetch(String url) throws FetchException
    {
        if (!isAllowed(url))
        {
            throw new FetchException("URL not allowed: " + url);
        }

        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "KWIC-Indexer/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2)
            {
                throw new FetchException("HTTP " + response.statusCode());
            }

            String contentType = response.headers()
                    .firstValue("content-type").orElse("");
            if (!contentType.isBlank()
                    && !contentType.contains("html")
                    && !contentType.contains("text"))
            {
                throw new FetchException("Unsupported content-type: " + contentType);
            }

            String body = response.body();
            if (body != null && body.length() > MAX_BYTES)
            {
                body = body.substring(0, MAX_BYTES);
            }
            return body == null ? "" : body;
        }
        catch (FetchException e)
        {
            throw e;
        }
        catch (java.io.IOException | InterruptedException e)
        {
            if (e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            throw new FetchException("Fetch failed: " + e.getMessage(), e);
        }
    }
}
