package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.se6362.kwic.index.UrlFetcher;

public class UrlFetcherGuardTest
{
    private final UrlFetcher fetcher = new UrlFetcher();

    @Test
    void allowsPublicHttpAndHttps()
    {
        assertTrue(fetcher.isAllowed("http://example.com/page"));
        assertTrue(fetcher.isAllowed("https://en.wikipedia.org/wiki/KWIC"));
    }

    @Test
    void blocksNonHttpSchemes()
    {
        assertFalse(fetcher.isAllowed("file:///etc/passwd"));
        assertFalse(fetcher.isAllowed("ftp://example.com"));
        assertFalse(fetcher.isAllowed("not a url"));
    }

    @Test
    void blocksLocalhostAndPrivateHosts()
    {
        assertFalse(fetcher.isAllowed("http://localhost:8080/admin"));
        assertFalse(fetcher.isAllowed("http://127.0.0.1/"));
        assertFalse(fetcher.isAllowed("http://10.0.0.5/"));
        assertFalse(fetcher.isAllowed("http://192.168.1.1/"));
        assertFalse(fetcher.isAllowed("http://169.254.169.254/latest/meta-data"));
    }

    @Test
    void blocksIpv6LoopbackAndUniqueLocal()
    {
        assertFalse(fetcher.isAllowed("http://[::1]/"));
        assertFalse(fetcher.isAllowed("http://[fd00::1]/"));
        assertFalse(fetcher.isAllowed("http://[fc00::1]/"));
    }

    @Test
    void blocksNullUrl()
    {
        assertFalse(fetcher.isAllowed(null));
    }
}
