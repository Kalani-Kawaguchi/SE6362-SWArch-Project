package com.se6362.kwic.index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

@Component
public class UrlFetcher
{
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_BYTES = 2_000_000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_PORT = 65535;

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

        // A syntactically valid URI can still carry an out-of-range port that
        // HttpClient would reject with IllegalArgumentException.
        if (uri.getPort() > MAX_PORT)
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

        // NOTE (known limitation): this is a time-of-check/time-of-use guard —
        // HttpClient re-resolves the hostname when it connects, so a hostile
        // DNS entry that changes between here and the connection (DNS
        // rebinding) could still reach a private address. Fully closing this
        // requires pinning the connection to the validated IP (which breaks
        // TLS SNI / virtual hosts) or routing through a trusted egress proxy;
        // both are out of scope here. Re-validating every redirect hop keeps
        // the window small.
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
            catch (IllegalArgumentException e)
            {
                // e.g. an out-of-range port that slipped past URI parsing.
                throw new FetchException("Invalid URL: " + current, e);
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
                try
                {
                    current = URI.create(current).resolve(location).toString();
                }
                catch (IllegalArgumentException e)
                {
                    throw new FetchException("Invalid redirect location: " + location, e);
                }
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

            byte[] body = readBounded(response.body());
            return new String(body, charsetOf(contentType));
        }

        throw new FetchException("Too many redirects for: " + url);
    }

    // Reads at most MAX_BYTES, bounded by a wall-clock deadline so a server that
    // stalls its body cannot hang the request (and its DB transaction). On
    // timeout the stream is closed, which unblocks the background read.
    private byte[] readBounded(InputStream body) throws FetchException
    {
        CompletableFuture<byte[]> future =
                CompletableFuture.supplyAsync(() -> readAll(body));
        try
        {
            return future.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            future.cancel(true);
            drainQuietly(body);
            throw new FetchException("Body read timed out");
        }
        catch (ExecutionException e)
        {
            drainQuietly(body);
            Throwable cause = e.getCause();
            throw new FetchException(
                    "Read failed: " + (cause == null ? e.getMessage() : cause.getMessage()), e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            drainQuietly(body);
            throw new FetchException("Interrupted while reading body", e);
        }
    }

    private byte[] readAll(InputStream body)
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
            return buffer.toByteArray();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    // Decode with the charset declared in the Content-Type header, falling back
    // to UTF-8, so non-UTF-8 pages aren't corrupted before indexing.
    private Charset charsetOf(String contentType)
    {
        for (String part : contentType.split(";"))
        {
            String token = part.trim();
            if (token.regionMatches(true, 0, "charset=", 0, 8))
            {
                String name = token.substring(8).trim().replace("\"", "");
                try
                {
                    return Charset.forName(name);
                }
                catch (RuntimeException ignored)
                {
                    // Unknown/invalid charset name; fall through to UTF-8.
                }
            }
        }
        return StandardCharsets.UTF_8;
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
