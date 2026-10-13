package com.se6362.kwic.index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class UrlFetcher
{
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_BYTES = 2_000_000;
    private static final int MAX_REDIRECTS = 5;

    // Redirects are NOT followed automatically: each hop must pass the SSRF
    // guard before we fetch it, so a public URL cannot redirect us to a
    // private/internal address.
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(TIMEOUT)
            .build();

    public boolean isAllowed(String url)
    {
        if (url == null)
        {
            return false;
        }

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
                if (isBlocked(address))
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

    private boolean isBlocked(InetAddress address)
    {
        if (address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress())
        {
            return true;
        }

        // IPv6 unique-local addresses (fc00::/7) are private but not covered by
        // isSiteLocalAddress(); block them explicitly.
        byte[] bytes = address.getAddress();
        if (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc)
        {
            return true;
        }

        return false;
    }

    /** Returns the response body as a string, or throws FetchException. */
    public String fetch(String url) throws FetchException
    {
        String current = url;

        for (int hop = 0; hop <= MAX_REDIRECTS; hop++)
        {
            if (!isAllowed(current))
            {
                throw new FetchException("URL not allowed: " + current);
            }

            HttpResponse<InputStream> response;
            try
            {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(current))
                        .timeout(TIMEOUT)
                        .header("User-Agent", "KWIC-Indexer/1.0")
                        .GET()
                        .build();

                response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            }
            catch (IOException | InterruptedException e)
            {
                if (e instanceof InterruptedException)
                {
                    Thread.currentThread().interrupt();
                }
                throw new FetchException("Fetch failed: " + e.getMessage(), e);
            }

            int status = response.statusCode();

            if (status / 100 == 3)
            {
                String location = response.headers().firstValue("location").orElse(null);
                drainQuietly(response.body());
                if (location == null || location.isBlank())
                {
                    throw new FetchException("Redirect without Location header");
                }
                // Resolve relative redirects against the current URL, then
                // loop so the next destination is re-validated by isAllowed.
                current = URI.create(current).resolve(location).toString();
                continue;
            }

            if (status / 100 != 2)
            {
                drainQuietly(response.body());
                throw new FetchException("HTTP " + status);
            }

            String contentType = response.headers().firstValue("content-type").orElse("");
            if (!contentType.isBlank()
                    && !contentType.contains("html")
                    && !contentType.contains("text"))
            {
                drainQuietly(response.body());
                throw new FetchException("Unsupported content-type: " + contentType);
            }

            return readBounded(response.body());
        }

        throw new FetchException("Too many redirects for: " + url);
    }

    // Reads at most MAX_BYTES from the stream, so an oversized response can
    // never exhaust memory, and closes the stream when done.
    private String readBounded(InputStream body) throws FetchException
    {
        try (InputStream stream = body)
        {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while (total < MAX_BYTES && (read = stream.read(chunk)) != -1)
            {
                int allowed = Math.min(read, MAX_BYTES - total);
                buffer.write(chunk, 0, allowed);
                total += allowed;
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new FetchException("Read failed: " + e.getMessage(), e);
        }
    }

    private void drainQuietly(InputStream body)
    {
        try
        {
            body.close();
        }
        catch (IOException ignored)
        {
            // Nothing actionable; the connection will be released.
        }
    }
}
