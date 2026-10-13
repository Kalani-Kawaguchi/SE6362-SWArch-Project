package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.se6362.kwic.core.LineStorage;

public class LineStorageUriTest
{
    @Test
    void skippedBlankLineDoesNotMisassignUri()
    {
        LineStorage storage = new LineStorage();
        storage.storeLine("real words here", URI.create("http://a"));
        // Punctuation-only line is cleaned to empty and must be skipped,
        // and must NOT reassign the URI of the previous line.
        storage.storeLine("!!! @@@ ###", URI.create("http://b"));

        assertEquals(1, storage.getLineCount());
        assertEquals("http://a", storage.getURI(0).toString());
    }

    @Test
    void blankLineOnEmptyStorageAssignsNothing()
    {
        LineStorage storage = new LineStorage();
        storage.storeLine("###", URI.create("http://a"));
        assertEquals(0, storage.getLineCount());
        assertNull(storage.getURI(0)); // out-of-range -> null per LineList.getURI
    }
}
