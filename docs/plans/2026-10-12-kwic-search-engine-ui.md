# KWIC Search Engine UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Let a user paste a list of URLs, have the backend fetch + KWIC-index their text into MySQL, and search that index from a search bar that returns source URLs each with their keyword-in-context lines.

**Architecture:** Two server-side steps. `POST /api/index` fetches each URL (stdlib `HttpClient`), extracts block-level text (Jsoup), runs the existing KWIC core (`CircularShift` → `Alphabetizer`) synchronously, and persists every rotation `(uri, first_word, line_text)` to MySQL (idempotent per URL). `GET /api/search?q=` does a case-insensitive prefix query on `first_word` and groups matches by URL. The React UI pivots to a Build-Index panel + a Search bar + grouped results; the old line-by-line view is removed.

**Tech Stack:** Java 21, Spring Boot 4.1.1 (Web MVC + Data JPA), MySQL (H2 for tests), Jsoup, `java.net.http.HttpClient`; React 19 + Parcel.

**Design doc:** `docs/plans/2026-10-12-kwic-search-engine-ui-design.md`

**Conventions:** Backend uses TDD with JUnit 5 (run `./mvnw test` from `backend/`). Frontend has no test runner (none will be added — YAGNI); frontend tasks are verified by running the app and screenshotting. Package root: `com.se6362.kwic`. Allman braces to match the existing code style.

---

## Task 1: Add dependencies (Jsoup + H2) and test datasource

**Why:** Jsoup parses HTML (no JDK HTML parser exists). H2 gives tests an embedded DB so `@DataJpaTest`/MVC tests and the existing `BackendApplicationTests` context load run without a real MySQL. Both are explicit, task-required dependency changes (see design doc dependency note).

**Files:**
- Modify: `backend/pom.xml` (dependencies section)
- Create: `backend/src/test/resources/application.properties`

**Step 1: Add dependencies to `backend/pom.xml`**

Inside `<dependencies>`, after the `jackson-databind` dependency, add:

```xml
		<dependency>
			<groupId>org.jsoup</groupId>
			<artifactId>jsoup</artifactId>
			<version>1.18.1</version>
		</dependency>

		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
```

**Step 2: Create `backend/src/test/resources/application.properties`**

```properties
spring.datasource.url=jdbc:h2:mem:kwic;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

**Step 3: Verify the project still builds and existing tests pass**

Run (from `backend/`): `./mvnw -q test`
Expected: BUILD SUCCESS — `LineListTest`, `KwicSystemTest`, `BackendApplicationTests` all green (the last now has an H2 datasource in tests).

**Step 4: Commit**

```bash
git add backend/pom.xml backend/src/test/resources/application.properties
git commit -m "build: add jsoup + H2(test) deps and test datasource"
```

---

## Task 2: Fix `storeLine(line, uri)` empty-line URI bug

**Why:** `LineStorage.storeLine(String)` silently skips lines that are blank after punctuation-stripping. `storeLine(String, URI)` then calls `setURI(getLineCount() - 1, uri)`, which attaches the URL to the *previous* line (or index -1) when the current line was skipped. The indexer relies on correct URL↔line pairing, so this must be correct.

**Files:**
- Modify: `backend/src/main/java/com/se6362/kwic/core/LineStorage.java`
- Test: `backend/src/test/java/com/se6362/kwic/LineStorageUriTest.java` (create)

**Step 1: Write the failing test**

Create `backend/src/test/java/com/se6362/kwic/LineStorageUriTest.java`:

```java
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
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=LineStorageUriTest test`
Expected: FAIL — first test gets `http://b` (or an exception) instead of `http://a`.

**Step 3: Fix `storeLine(String, URI)`**

Replace the `storeLine(String line, URI uri)` method in `LineStorage.java` with:

```java
    public void storeLine(String line, URI uri)
    {
        int before = getLineCount();
        storeLine(line);
        if (getLineCount() > before)
        {
            setURI(getLineCount() - 1, uri);
        }
    }
```

**Step 4: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=LineStorageUriTest,LineListTest test`
Expected: PASS (all).

**Step 5: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/core/LineStorage.java backend/src/test/java/com/se6362/kwic/LineStorageUriTest.java
git commit -m "fix: don't misassign URI when a cleaned line is skipped"
```

---

## Task 3: Expose rotations from the KWIC index

**Why:** The service needs, per rotation, its `(uri, firstWord, text)` to persist. `LineList` only exposes word-by-word getters and a dump-everything `toString()`. Add one public accessor returning structured rows. Same mechanism works for any subclass (`Alphabetizer`, etc.).

**Files:**
- Create: `backend/src/main/java/com/se6362/kwic/core/Rotation.java`
- Modify: `backend/src/main/java/com/se6362/kwic/core/LineList.java`
- Test: `backend/src/test/java/com/se6362/kwic/RotationExtractionTest.java` (create)

**Step 1: Write the failing test**

Create `backend/src/test/java/com/se6362/kwic/RotationExtractionTest.java`:

```java
package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.se6362.kwic.core.CircularShift;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.core.Rotation;

public class RotationExtractionTest
{
    @Test
    void rotationsCarryFirstWordTextAndUri()
    {
        LineStorage storage = new LineStorage();
        storage.storeLine("quick brown fox", URI.create("http://a"));
        CircularShift shifts = new CircularShift();
        shifts.circularShiftLines(storage);

        List<Rotation> rotations = shifts.getRotations();

        assertEquals(3, rotations.size());
        assertEquals("quick", rotations.get(0).firstWord());
        assertEquals("quick brown fox", rotations.get(0).text());
        assertEquals("http://a", rotations.get(0).uri());
        assertEquals("brown", rotations.get(1).firstWord());
        assertEquals("brown fox quick", rotations.get(1).text());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=RotationExtractionTest test`
Expected: FAIL — `Rotation` / `getRotations` not defined (compile error).

**Step 3: Create `Rotation.java`**

```java
package com.se6362.kwic.core;

public record Rotation(String uri, String firstWord, String text)
{
}
```

**Step 4: Add `getRotations()` to `LineList.java`**

Add these imports if missing (`java.util.ArrayList`, `java.util.List` are already imported) and add this public method to `LineList`:

```java
    public List<Rotation> getRotations()
    {
        List<Rotation> rotations = new ArrayList<>();
        for (Line line : lines)
        {
            int wordCount = line.getWordCount();
            if (wordCount == 0)
            {
                continue;
            }
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < wordCount; i++)
            {
                if (i > 0)
                {
                    text.append(' ');
                }
                text.append(line.getWord(i));
            }
            URI uri = line.getURI();
            rotations.add(new Rotation(
                    uri == null ? null : uri.toString(),
                    line.getWord(0),
                    text.toString()));
        }
        return rotations;
    }
```

**Step 5: Run test to verify it passes**

Run: `./mvnw -q -Dtest=RotationExtractionTest test`
Expected: PASS.

**Step 6: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/core/Rotation.java backend/src/main/java/com/se6362/kwic/core/LineList.java backend/src/test/java/com/se6362/kwic/RotationExtractionTest.java
git commit -m "feat: expose structured rotations (uri, firstWord, text) from LineList"
```

---

## Task 4: `KwicLine` entity + repository

**Files:**
- Create: `backend/src/main/java/com/se6362/kwic/index/KwicLine.java`
- Create: `backend/src/main/java/com/se6362/kwic/index/KwicLineRepository.java`
- Test: `backend/src/test/java/com/se6362/kwic/KwicLineRepositoryTest.java` (create)

**Step 1: Write the failing test**

Create `backend/src/test/java/com/se6362/kwic/KwicLineRepositoryTest.java`:

```java
package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.se6362.kwic.index.KwicLine;
import com.se6362.kwic.index.KwicLineRepository;

@DataJpaTest
public class KwicLineRepositoryTest
{
    @Autowired
    KwicLineRepository repository;

    @Test
    void prefixSearchIsCaseInsensitiveAndOrdered()
    {
        repository.save(new KwicLine("http://b", "software", "software architecture is"));
        repository.save(new KwicLine("http://b", "engineering", "engineering is the process"));
        repository.save(new KwicLine("http://c", "Soft", "Soft touch here"));

        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("soft");

        assertEquals(2, hits.size());
        assertTrue(hits.get(0).getFirstWord().equalsIgnoreCase("soft")
                || hits.get(0).getFirstWord().equalsIgnoreCase("software"));
    }

    @Test
    void deleteByUriInRemovesOnlyThoseUrls()
    {
        repository.save(new KwicLine("http://b", "alpha", "alpha one"));
        repository.save(new KwicLine("http://c", "alpha", "alpha two"));

        repository.deleteByUriIn(List.of("http://b"));

        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("alpha");
        assertEquals(1, hits.size());
        assertEquals("http://c", hits.get(0).getUri());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=KwicLineRepositoryTest test`
Expected: FAIL — classes not defined.

**Step 3: Create `KwicLine.java`**

```java
package com.se6362.kwic.index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "kwic_line", indexes = {
        @Index(name = "idx_first_word", columnList = "first_word"),
        @Index(name = "idx_uri", columnList = "uri")
})
public class KwicLine
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uri", length = 2048)
    private String uri;

    @Column(name = "first_word", length = 255)
    private String firstWord;

    @Column(name = "line_text", length = 1024)
    private String lineText;

    protected KwicLine()
    {
    }

    public KwicLine(String uri, String firstWord, String lineText)
    {
        this.uri = uri;
        this.firstWord = firstWord;
        this.lineText = lineText;
    }

    public Long getId()
    {
        return id;
    }

    public String getUri()
    {
        return uri;
    }

    public String getFirstWord()
    {
        return firstWord;
    }

    public String getLineText()
    {
        return lineText;
    }
}
```

**Step 4: Create `KwicLineRepository.java`**

```java
package com.se6362.kwic.index;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KwicLineRepository extends JpaRepository<KwicLine, Long>
{
    List<KwicLine> findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc(String prefix);

    void deleteByUriIn(Collection<String> uris);
}
```

**Step 5: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=KwicLineRepositoryTest test`
Expected: PASS (both).

**Step 6: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/index/ backend/src/test/java/com/se6362/kwic/KwicLineRepositoryTest.java
git commit -m "feat: KwicLine entity + repository with prefix search and delete-by-uri"
```

---

## Task 5: `HtmlTextExtractor` (Jsoup)

**Why:** Turn a fetched HTML page into a list of clean, block-level "lines" to feed KWIC.

**Files:**
- Create: `backend/src/main/java/com/se6362/kwic/index/HtmlTextExtractor.java`
- Test: `backend/src/test/java/com/se6362/kwic/HtmlTextExtractorTest.java` (create)

**Step 1: Write the failing test**

```java
package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.se6362.kwic.index.HtmlTextExtractor;

public class HtmlTextExtractorTest
{
    @Test
    void extractsBlockTextAndDropsScriptAndStyle()
    {
        String html = "<html><head><title>My Page</title>"
                + "<style>.x{color:red}</style></head><body>"
                + "<h1>Hello World</h1>"
                + "<p>The quick brown fox</p>"
                + "<script>var a = 1;</script>"
                + "<ul><li>first item</li><li>second item</li></ul>"
                + "</body></html>";

        List<String> lines = new HtmlTextExtractor().extractLines(html);

        assertTrue(lines.contains("My Page"));
        assertTrue(lines.contains("Hello World"));
        assertTrue(lines.contains("The quick brown fox"));
        assertTrue(lines.contains("first item"));
        assertFalse(lines.stream().anyMatch(l -> l.contains("var a")));
        assertFalse(lines.stream().anyMatch(l -> l.contains("color:red")));
    }

    @Test
    void skipsVeryLongLinesAndBlanks()
    {
        StringBuilder big = new StringBuilder("<p>");
        for (int i = 0; i < 200; i++)
        {
            big.append("word ");
        }
        big.append("</p><p>   </p><p>keep me</p>");

        List<String> lines = new HtmlTextExtractor().extractLines(big.toString());

        assertTrue(lines.contains("keep me"));
        assertFalse(lines.stream().anyMatch(l -> l.split("\\s+").length > 50));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=HtmlTextExtractorTest test`
Expected: FAIL — class not defined.

**Step 3: Create `HtmlTextExtractor.java`**

```java
package com.se6362.kwic.index;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlTextExtractor
{
    private static final int MAX_WORDS_PER_LINE = 50;
    private static final int MAX_LINES_PER_DOC = 500;
    private static final String BLOCK_SELECTOR = "title, h1, h2, h3, h4, h5, h6, p, li";

    public List<String> extractLines(String html)
    {
        List<String> lines = new ArrayList<>();
        if (html == null || html.isBlank())
        {
            return lines;
        }

        Document document = Jsoup.parse(html);
        document.select("script, style, noscript").remove();

        for (Element element : document.select(BLOCK_SELECTOR))
        {
            String text = element.ownText().trim();
            if (text.isEmpty())
            {
                text = element.text().trim();
            }
            if (text.isEmpty())
            {
                continue;
            }
            if (text.split("\\s+").length > MAX_WORDS_PER_LINE)
            {
                continue;
            }
            lines.add(text);
            if (lines.size() >= MAX_LINES_PER_DOC)
            {
                break;
            }
        }
        return lines;
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=HtmlTextExtractorTest test`
Expected: PASS.

**Step 5: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/index/HtmlTextExtractor.java backend/src/test/java/com/se6362/kwic/HtmlTextExtractorTest.java
git commit -m "feat: Jsoup-based HTML to block-level text extractor"
```

---

## Task 6: `UrlFetcher` with SSRF guard

**Why:** Fetch page HTML over HTTP, and refuse unsafe targets (non-http(s) schemes, localhost/private IPs). The guard is the security-critical, unit-testable part; the HTTP call itself is thin.

**Files:**
- Create: `backend/src/main/java/com/se6362/kwic/index/UrlFetcher.java`
- Create: `backend/src/main/java/com/se6362/kwic/index/FetchException.java`
- Test: `backend/src/test/java/com/se6362/kwic/UrlFetcherGuardTest.java` (create)

**Step 1: Write the failing test** (guard logic only — no network)

```java
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
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=UrlFetcherGuardTest test`
Expected: FAIL — class not defined.

**Step 3: Create `FetchException.java`**

```java
package com.se6362.kwic.index;

public class FetchException extends Exception
{
    public FetchException(String message)
    {
        super(message);
    }

    public FetchException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
```

**Step 4: Create `UrlFetcher.java`**

```java
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
```

**Step 5: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=UrlFetcherGuardTest test`
Expected: PASS. (Guard tests do DNS resolution for public hosts; if the build host is offline, `isAllowed` for public hosts may return false — note this and re-run when online.)

**Step 6: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/index/UrlFetcher.java backend/src/main/java/com/se6362/kwic/index/FetchException.java backend/src/test/java/com/se6362/kwic/UrlFetcherGuardTest.java
git commit -m "feat: UrlFetcher with SSRF guard (scheme + private-IP block)"
```

---

## Task 7: `KwicIndexService` — orchestrate fetch → extract → KWIC → persist

**Why:** The core use case. Fetches each URL, extracts lines, runs KWIC synchronously, and persists rotations idempotently per URL. Returns a per-URL summary.

**Files:**
- Create: `backend/src/main/java/com/se6362/kwic/index/IndexResult.java`
- Create: `backend/src/main/java/com/se6362/kwic/index/KwicIndexService.java`
- Test: `backend/src/test/java/com/se6362/kwic/KwicIndexServiceTest.java` (create)

**Step 1: Write the failing test** (mock the fetcher, real H2 repository)

```java
package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.se6362.kwic.index.FetchException;
import com.se6362.kwic.index.HtmlTextExtractor;
import com.se6362.kwic.index.IndexResult;
import com.se6362.kwic.index.KwicIndexService;
import com.se6362.kwic.index.KwicLine;
import com.se6362.kwic.index.KwicLineRepository;
import com.se6362.kwic.index.UrlFetcher;

@DataJpaTest
@Import({ KwicIndexService.class, HtmlTextExtractor.class })
public class KwicIndexServiceTest
{
    @Autowired
    KwicLineRepository repository;

    @Autowired
    KwicIndexService service;

    @MockitoBean
    UrlFetcher fetcher;

    @Test
    void indexesAndPersistsRotationsWithUrls() throws FetchException
    {
        when(fetcher.fetch("http://a")).thenReturn("<p>quick brown fox</p>");

        IndexResult result = service.index(List.of("http://a"));

        assertEquals(1, result.indexed().size());
        assertEquals("http://a", result.indexed().get(0).url());
        // "quick brown fox" -> 3 rotations
        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("brown");
        assertEquals(1, hits.size());
        assertEquals("http://a", hits.get(0).getUri());
        assertEquals("brown fox quick", hits.get(0).getLineText());
    }

    @Test
    void reindexingSameUrlReplacesRows() throws FetchException
    {
        when(fetcher.fetch("http://a")).thenReturn("<p>alpha beta</p>");
        service.index(List.of("http://a"));

        when(fetcher.fetch("http://a")).thenReturn("<p>gamma delta</p>");
        service.index(List.of("http://a"));

        assertTrue(repository
                .findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("alpha").isEmpty());
        assertEquals(1, repository
                .findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc("gamma").size());
    }

    @Test
    void failedFetchIsReportedNotThrown() throws FetchException
    {
        when(fetcher.fetch("http://bad")).thenThrow(new FetchException("timeout"));

        IndexResult result = service.index(List.of("http://bad"));

        assertTrue(result.indexed().isEmpty());
        assertEquals(1, result.failed().size());
        assertEquals("http://bad", result.failed().get(0).url());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./mvnw -q -Dtest=KwicIndexServiceTest test`
Expected: FAIL — classes not defined.

**Step 3: Create `IndexResult.java`**

```java
package com.se6362.kwic.index;

import java.util.List;

public record IndexResult(
        List<Indexed> indexed,
        List<Failed> failed,
        int totalRotations)
{
    public record Indexed(String url, int lineCount)
    {
    }

    public record Failed(String url, String error)
    {
    }
}
```

**Step 4: Create `KwicIndexService.java`**

```java
package com.se6362.kwic.index;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.se6362.kwic.core.Alphabetizer;
import com.se6362.kwic.core.CircularShift;
import com.se6362.kwic.core.LineStorage;
import com.se6362.kwic.core.Rotation;

@Service
public class KwicIndexService
{
    private static final int MAX_URLS = 20;

    private final UrlFetcher fetcher;
    private final HtmlTextExtractor extractor;
    private final KwicLineRepository repository;

    public KwicIndexService(
            UrlFetcher fetcher,
            HtmlTextExtractor extractor,
            KwicLineRepository repository)
    {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.repository = repository;
    }

    @Transactional
    public IndexResult index(List<String> urls)
    {
        List<IndexResult.Indexed> indexed = new ArrayList<>();
        List<IndexResult.Failed> failed = new ArrayList<>();
        int totalRotations = 0;

        List<String> limited = urls.size() > MAX_URLS
                ? urls.subList(0, MAX_URLS)
                : urls;

        for (String url : limited)
        {
            try
            {
                String html = fetcher.fetch(url);
                List<String> lines = extractor.extractLines(html);

                if (lines.isEmpty())
                {
                    failed.add(new IndexResult.Failed(url, "no indexable text found"));
                    continue;
                }

                List<Rotation> rotations = buildRotations(url, lines);

                repository.deleteByUriIn(List.of(url));
                List<KwicLine> rows = new ArrayList<>();
                for (Rotation rotation : rotations)
                {
                    rows.add(new KwicLine(
                            rotation.uri(),
                            rotation.firstWord(),
                            rotation.text()));
                }
                repository.saveAll(rows);

                indexed.add(new IndexResult.Indexed(url, lines.size()));
                totalRotations += rows.size();
            }
            catch (FetchException e)
            {
                failed.add(new IndexResult.Failed(url, e.getMessage()));
            }
        }

        return new IndexResult(indexed, failed, totalRotations);
    }

    private List<Rotation> buildRotations(String url, List<String> lines)
    {
        LineStorage storage = new LineStorage();
        URI uri = URI.create(url);
        for (String line : lines)
        {
            storage.storeLine(line, uri);
        }
        CircularShift shifts = new CircularShift();
        shifts.circularShiftLines(storage);
        Alphabetizer alphabetizer = new Alphabetizer();
        alphabetizer.alphabetizeLines(shifts);
        return alphabetizer.getRotations();
    }
}
```

**Step 5: Run tests to verify they pass**

Run: `./mvnw -q -Dtest=KwicIndexServiceTest test`
Expected: PASS (all three).

**Step 6: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/index/IndexResult.java backend/src/main/java/com/se6362/kwic/index/KwicIndexService.java backend/src/test/java/com/se6362/kwic/KwicIndexServiceTest.java
git commit -m "feat: KwicIndexService (fetch->extract->KWIC->persist, idempotent per URL)"
```

---

## Task 8: Search service + controller endpoints

**Why:** Expose `POST /api/index` and `GET /api/search?q=` and group search hits by URL.

**Files:**
- Create: `backend/src/main/java/com/se6362/kwic/index/SearchResult.java`
- Create: `backend/src/main/java/com/se6362/kwic/index/SearchService.java`
- Create: `backend/src/main/java/com/se6362/kwic/controller/IndexController.java`
- Test: `backend/src/test/java/com/se6362/kwic/SearchServiceTest.java` (create)
- Test: `backend/src/test/java/com/se6362/kwic/IndexControllerTest.java` (create)

**Step 1: Write the failing SearchService test**

```java
package com.se6362.kwic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.se6362.kwic.index.KwicLine;
import com.se6362.kwic.index.KwicLineRepository;
import com.se6362.kwic.index.SearchResult;
import com.se6362.kwic.index.SearchService;

@DataJpaTest
@Import(SearchService.class)
public class SearchServiceTest
{
    @Autowired
    KwicLineRepository repository;

    @Autowired
    SearchService service;

    @Test
    void groupsResultsByUrlPreservingOrder()
    {
        repository.save(new KwicLine("http://b", "software", "software architecture is"));
        repository.save(new KwicLine("http://b", "software", "software engineering is"));
        repository.save(new KwicLine("http://d", "software", "software systems design"));

        SearchResult result = service.search("soft");

        assertEquals(3, result.resultCount());
        assertEquals(2, result.groups().size());
        assertEquals("http://b", result.groups().get(0).url());
        assertEquals(2, result.groups().get(0).contexts().size());
        assertEquals("http://d", result.groups().get(1).url());
    }

    @Test
    void emptyQueryReturnsNoResults()
    {
        SearchResult result = service.search("  ");
        assertEquals(0, result.resultCount());
        assertEquals(0, result.groups().size());
    }
}
```

**Step 2: Run to verify it fails**

Run: `./mvnw -q -Dtest=SearchServiceTest test`
Expected: FAIL — classes not defined.

**Step 3: Create `SearchResult.java`**

```java
package com.se6362.kwic.index;

import java.util.List;

public record SearchResult(
        String query,
        int resultCount,
        List<Group> groups)
{
    public record Group(String url, List<String> contexts)
    {
    }
}
```

**Step 4: Create `SearchService.java`**

```java
package com.se6362.kwic.index;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SearchService
{
    private final KwicLineRepository repository;

    public SearchService(KwicLineRepository repository)
    {
        this.repository = repository;
    }

    public SearchResult search(String query)
    {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty())
        {
            return new SearchResult(q, 0, List.of());
        }

        List<KwicLine> hits =
                repository.findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc(q);

        Map<String, List<String>> byUrl = new LinkedHashMap<>();
        for (KwicLine hit : hits)
        {
            byUrl.computeIfAbsent(hit.getUri(), k -> new ArrayList<>())
                    .add(hit.getLineText());
        }

        List<SearchResult.Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byUrl.entrySet())
        {
            groups.add(new SearchResult.Group(entry.getKey(), entry.getValue()));
        }

        return new SearchResult(q, hits.size(), groups);
    }
}
```

**Step 5: Run SearchService test**

Run: `./mvnw -q -Dtest=SearchServiceTest test`
Expected: PASS.

**Step 6: Create `IndexController.java`**

```java
package com.se6362.kwic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.se6362.kwic.index.IndexResult;
import com.se6362.kwic.index.KwicIndexService;
import com.se6362.kwic.index.SearchResult;
import com.se6362.kwic.index.SearchService;

@CrossOrigin(origins = { "https://kalani-kawaguchi.github.io", "http://localhost:1234" })
@RestController
@RequestMapping("/api")
public class IndexController
{
    private final KwicIndexService indexService;
    private final SearchService searchService;

    public IndexController(KwicIndexService indexService, SearchService searchService)
    {
        this.indexService = indexService;
        this.searchService = searchService;
    }

    @PostMapping("/index")
    public IndexResult index(@RequestBody IndexRequest request)
    {
        if (request == null || request.urls() == null || request.urls().isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one URL is required");
        }
        return indexService.index(request.urls());
    }

    @GetMapping("/search")
    public SearchResult search(@RequestParam(name = "q", required = false) String q)
    {
        if (q == null || q.isBlank())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query 'q' is required");
        }
        return searchService.search(q);
    }

    public record IndexRequest(List<String> urls)
    {
    }
}
```

**Step 7: Write the failing controller (MVC) test**

```java
package com.se6362.kwic;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.se6362.kwic.controller.IndexController;
import com.se6362.kwic.index.IndexResult;
import com.se6362.kwic.index.KwicIndexService;
import com.se6362.kwic.index.SearchResult;
import com.se6362.kwic.index.SearchService;

@WebMvcTest(IndexController.class)
public class IndexControllerTest
{
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KwicIndexService indexService;

    @MockitoBean
    SearchService searchService;

    @Test
    void searchReturnsGroupedJson() throws Exception
    {
        when(searchService.search("soft")).thenReturn(new SearchResult(
                "soft", 2,
                List.of(new SearchResult.Group("http://b",
                        List.of("software architecture is")))));

        mockMvc.perform(get("/api/search").param("q", "soft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("soft"))
                .andExpect(jsonPath("$.groups[0].url").value("http://b"));
    }

    @Test
    void searchWithoutQueryIsBadRequest() throws Exception
    {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void indexReturnsSummary() throws Exception
    {
        when(indexService.index(anyList())).thenReturn(
                new IndexResult(List.of(new IndexResult.Indexed("http://a", 3)),
                        List.of(), 9));

        mockMvc.perform(post("/api/index")
                .contentType("application/json")
                .content("{\"urls\":[\"http://a\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRotations").value(9))
                .andExpect(jsonPath("$.indexed[0].url").value("http://a"));
    }

    @Test
    void indexWithNoUrlsIsBadRequest() throws Exception
    {
        mockMvc.perform(post("/api/index")
                .contentType("application/json")
                .content("{\"urls\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
```

**Step 8: Run the controller + search tests**

Run: `./mvnw -q -Dtest=SearchServiceTest,IndexControllerTest test`
Expected: PASS (all).

**Step 9: Full backend test run**

Run: `./mvnw -q test`
Expected: BUILD SUCCESS, all suites green.

**Step 10: Commit**

```bash
git add backend/src/main/java/com/se6362/kwic/index/SearchResult.java backend/src/main/java/com/se6362/kwic/index/SearchService.java backend/src/main/java/com/se6362/kwic/controller/IndexController.java backend/src/test/java/com/se6362/kwic/SearchServiceTest.java backend/src/test/java/com/se6362/kwic/IndexControllerTest.java
git commit -m "feat: /api/index and /api/search endpoints with grouped-by-URL results"
```

---

## Task 9: Frontend — Build Index panel + Search bar

**Why:** Replace the paste-text flow with URL ingestion + search. Reuse existing design system.

**Files:**
- Modify: `frontend/src/KWIC2.jsx` (replace body with index + search UI)
- Create: `frontend/src/SearchResults.jsx`
- Modify: `frontend/src/styles.css` (add search/result styles)
- Modify: `frontend/src/App.jsx` (unchanged render of `<KWIC2 />`; confirm no `KwicResultRow` import remains)

**Step 1: Create `frontend/src/SearchResults.jsx`**

```jsx
function highlight(text, query) {
  if (!query) return text;
  const lower = text.toLowerCase();
  const q = query.toLowerCase();
  const parts = [];
  let i = 0;
  while (i < text.length) {
    const idx = lower.indexOf(q, i);
    if (idx === -1) {
      parts.push(text.slice(i));
      break;
    }
    if (idx > i) parts.push(text.slice(i, idx));
    parts.push(<mark key={idx}>{text.slice(idx, idx + q.length)}</mark>);
    i = idx + q.length;
  }
  return parts;
}

export default function SearchResults({ query, result, isSearching }) {
  if (isSearching) {
    return <p className="result-summary" role="status">Searching…</p>;
  }

  if (!result) {
    return (
      <p className="result-summary">
        Build an index, then search a keyword to see matching pages.
      </p>
    );
  }

  if (result.resultCount === 0) {
    return (
      <p className="result-summary" role="status">
        No matches for “{result.query}”.
      </p>
    );
  }

  return (
    <div className="search-results">
      <p className="result-summary" role="status">
        {result.resultCount} match{result.resultCount === 1 ? "" : "es"} across{" "}
        {result.groups.length} page{result.groups.length === 1 ? "" : "s"}
      </p>

      {result.groups.map((group) => (
        <section className="result-group" key={group.url}>
          <a className="result-url" href={group.url} target="_blank" rel="noreferrer">
            {group.url}
          </a>
          <ul className="result-contexts">
            {group.contexts.map((line, index) => (
              <li key={index}>{highlight(line, query)}</li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
```

**Step 2: Replace `frontend/src/KWIC2.jsx` with the index + search UI**

Full replacement contents:

```jsx
import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import { useState } from "react";
import "./styles.css";
import SearchResults from "./SearchResults";

const API_BASE_URL = (process.env.API_BASE_URL || "http://localhost:8080")
  .replace(/\/+$/, "");

export default function KWIC2() {
  const [urls, setUrls] = useState("");
  const [indexStatus, setIndexStatus] = useState(null);
  const [isIndexing, setIsIndexing] = useState(false);
  const [indexError, setIndexError] = useState("");

  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [searchResult, setSearchResult] = useState(null);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState("");

  async function buildIndex(event) {
    event.preventDefault();
    if (isIndexing) return;

    const list = urls
      .split(/\r?\n/)
      .map((u) => u.trim())
      .filter(Boolean);

    if (list.length === 0) {
      setIndexError("Enter at least one URL.");
      return;
    }

    setIndexError("");
    setIndexStatus(null);
    setIsIndexing(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/index`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ urls: list }),
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      setIndexStatus(await response.json());
    } catch (err) {
      console.error("Index build failed:", err);
      setIndexError("Couldn't reach the backend to build the index.");
    } finally {
      setIsIndexing(false);
    }
  }

  async function runSearch(event) {
    event.preventDefault();
    if (isSearching) return;

    const q = query.trim();
    if (!q) {
      setSearchError("Enter a keyword to search.");
      return;
    }

    setSearchError("");
    setIsSearching(true);
    setSubmittedQuery(q);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/search?q=${encodeURIComponent(q)}`
      );
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      setSearchResult(await response.json());
    } catch (err) {
      console.error("Search failed:", err);
      setSearchError("Couldn't reach the backend to search.");
      setSearchResult(null);
    } finally {
      setIsSearching(false);
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main">
          <span className="brand-icon" aria-hidden="true">K</span>
          KWIC
          <span className="brand-detail">/ Search</span>
        </a>
      </header>

      <main id="main">
        <section className="intro" aria-labelledby="page-title">
          <p className="eyebrow">KEY WORD IN CONTEXT</p>
          <h1 id="page-title">
            Search the web,
            <br />
            <span>in context.</span>
          </h1>
        </section>

        {/* Search bar */}
        <form className="search-bar" onSubmit={runSearch} role="search">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search a keyword (e.g. software)"
            aria-label="Search keyword"
            disabled={isSearching}
            spellCheck="false"
          />
          <button className="primary-button" type="submit" disabled={isSearching}>
            {isSearching ? "Searching…" : "Search"}
          </button>
        </form>
        {searchError && <p className="error" role="alert">{searchError}</p>}

        <SearchResults
          query={submittedQuery}
          result={searchResult}
          isSearching={isSearching}
        />

        {/* Build index */}
        <section className="panel input-panel" aria-labelledby="index-heading">
          <div className="panel-heading">
            <h2 id="index-heading">Build the index</h2>
            <span className="tag">URLS</span>
          </div>

          <form onSubmit={buildIndex}>
            <label htmlFor="url-input">Pages to index</label>
            <p className="help">One URL per line. The backend fetches each page and indexes its text.</p>
            <textarea
              id="url-input"
              value={urls}
              onChange={(e) => { setUrls(e.target.value); setIndexError(""); }}
              placeholder={"https://en.wikipedia.org/wiki/Key_Word_in_Context\nhttps://example.com/article"}
              rows={5}
              disabled={isIndexing}
              spellCheck="false"
            />
            {indexError && <p className="error" role="alert">{indexError}</p>}
            <div className="actions">
              <button className="primary-button" type="submit"
                disabled={!urls.trim() || isIndexing}>
                {isIndexing ? "Indexing…" : "Build index"} <span aria-hidden="true"> →</span>
              </button>
            </div>
          </form>

          {indexStatus && (
            <ul className="index-status">
              {indexStatus.indexed.map((d) => (
                <li className="ok" key={`ok-${d.url}`}>✓ {d.url} — {d.lineCount} lines</li>
              ))}
              {indexStatus.failed.map((f) => (
                <li className="fail" key={`fail-${f.url}`}>✗ {f.url} — {f.error}</li>
              ))}
            </ul>
          )}
        </section>
      </main>

      <footer>
        <div>
          <strong>SE 6362 · Software Architecture</strong>
          <p>Kalani Kawaguchi · Zhi Li · Jonathan Loper · Mitchell Vu</p>
        </div>
        <a href={ProjPlan}>Preliminary project plan ↗</a>
      </footer>
    </div>
  );
}
```

**Step 3: Append styles to `frontend/src/styles.css`**

```css
.search-bar {
  display: flex;
  gap: 12px;
  margin: 24px 0;
}

.search-bar input {
  flex: 1;
  font-size: 18px;
  color: #213c39;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 14px 18px;
  background: #fff;
}

.search-results {
  margin-bottom: 40px;
}

.result-group {
  padding: 16px 0;
  border-bottom: 1px solid #d9e1d9;
}

.result-url {
  display: inline-block;
  font: 600 15px "Space Grotesk", sans-serif;
  color: #067965;
  margin-bottom: 8px;
  word-break: break-all;
}

.result-contexts {
  margin: 0;
  padding-left: 18px;
  color: #213c39;
}

.result-contexts li {
  margin: 4px 0;
  line-height: 1.5;
}

.result-contexts mark {
  background: #cdeee4;
  color: inherit;
  padding: 0 2px;
  border-radius: 3px;
}

.index-status {
  list-style: none;
  padding: 0;
  margin: 16px 0 0;
  font-size: 14px;
}

.index-status .ok { color: #067965; }
.index-status .fail { color: #b42318; }
.index-status li { margin: 4px 0; word-break: break-all; }
```

**Step 4: Verify no dead references remain**

Run: `grep -n "KwicResultRow\|incrementalResults\|ResultPanel" frontend/src/KWIC2.jsx`
Expected: no output (all removed from the main flow). `KWIC.jsx`, `KwicResultRow.jsx`, `ResultPanel.jsx` may remain on disk but are no longer imported by `KWIC2.jsx`.

**Step 5: Commit**

```bash
git add frontend/src/KWIC2.jsx frontend/src/SearchResults.jsx frontend/src/styles.css
git commit -m "feat: search bar + URL index panel + grouped results UI"
```

---

## Task 10: End-to-end verification

**Why:** Prove the full flow works against a running backend + frontend. REQUIRED SKILL: use the `verify` skill if available.

**Step 1: Start the backend with an H2 dev profile (avoids needing MySQL locally)**

Create `backend/src/main/resources/application-dev.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:kwicdev;DB_CLOSE_DELAY=-1;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

Run (from `backend/`): `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
Expected: starts on port 8080. (Production still uses MySQL via env vars — this profile is dev-only and must NOT become the default.)

**Step 2: Smoke-test the API with curl**

```bash
curl -s -X POST http://localhost:8080/api/index \
  -H 'Content-Type: application/json' \
  -d '{"urls":["https://en.wikipedia.org/wiki/Key_Word_in_Context"]}' | head -c 400
curl -s 'http://localhost:8080/api/search?q=context' | head -c 400
```
Expected: index returns `{"indexed":[...],"failed":[],"totalRotations":N}`; search returns grouped JSON with the Wikipedia URL.

**Step 3: Run the frontend**

Run (from `frontend/`): set `API_BASE_URL=http://localhost:8080` (e.g. in `.env` / `.env.development`) then `npm install && npm start`.
Expected: Parcel serves on `http://localhost:1234`.

**Step 4: Drive the UI and screenshot (use the `run` or `claude-in-chrome` skill)**

- Paste a URL, click **Build index**, confirm a ✓ status row appears.
- Type a keyword present on the page, click **Search**, confirm results are grouped by URL with the keyword highlighted.
- Verify empty-query and no-results states.

**Step 5: Final commit (dev profile)**

```bash
git add backend/src/main/resources/application-dev.properties
git commit -m "chore: add H2 dev profile for local run"
```

---

## Notes for production (MySQL)

Railway provides the MySQL datasource via env vars (`SPRING_DATASOURCE_URL`, etc.) — nothing to change there. On first deploy with the new entity, ensure `spring.jpa.hibernate.ddl-auto` is set appropriately for prod (e.g. `update`) via Railway env, not committed to `application.properties`. Flag this in the PR.

**MANDATORY before any push (per repo policy): run `codex review --base main` and share output.** Do not push without it.
```
