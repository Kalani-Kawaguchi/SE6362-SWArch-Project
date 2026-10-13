# KWIC Search Engine UI — Design

Date: 2026-10-12
Status: Approved (brainstorming → ready for implementation plan)

## Goal

Turn the KWIC app from "paste text → see every rotation" into a small search engine:
a user supplies a list of URLs, the backend fetches and KWIC-indexes their content,
and a search bar queries that index — returning a list of source URLs, each with the
keyword-in-context lines (the KWIC rotations) that matched.

## Approved decisions

- **Data source:** backend fetches real URLs supplied by the user.
- **Search model:** server-side. Indexing and search are separate steps.
- **Index storage:** MySQL-persisted (fills the existing `OutputBuffer.writeToDatabase()`
  stub; uses the already-provisioned Railway MySQL + `spring-boot-starter-data-jpa`).
- **Match semantics:** case-insensitive **prefix** match on the leading word of each
  rotation (classic KWIC), results **grouped by URL**.
- **HTML extraction:** add **Jsoup** (`org.jsoup:jsoup`) — NEW dependency, justified below.
- **Incremental line-by-line view:** removed (was effectively a debug view; the search
  UI replaces it as the primary experience).
- **SSRF guard:** allow only `http`/`https` schemes and block localhost/private IP ranges.

## Architecture & data flow

```
Build:   UI (URL list) --POST /api/index--> fetch each URL (java.net.http.HttpClient)
                                            -> extract text (Jsoup) -> split into lines
                                            -> KWIC pipeline (CircularShift -> Alphabetizer -> MergedLines)
                                            -> each rotation carries its source URL
                                            -> DELETE old rows for these URLs, INSERT rotations
                                            <-- summary { indexed[], failed[], totalRotations }

Search:  UI (search bar) --GET /api/search?q=soft--> repo.findByFirstWordStartingWithIgnoreCase(...)
                                            -> group rows by URL
                                            <-- { query, resultCount, groups: [ {url, contexts[]} ] }
```

The KWIC core (`CircularShift`, `Alphabetizer`, `MergedLines`, `LineList`, `Line`) is reused
**unchanged**. `Line`/`CircularShift` already propagate a per-line `URI` through every rotation,
so once ingestion attaches the source URL, every rotation in the merged index already knows
where it came from. The only core-path change is threading the URL through ingestion
(`InputBuffer` currently reads `{text}` and drops any URI).

## API contract

### `POST /api/index`
Request:
```json
{ "urls": ["https://example.com/a", "https://example.com/b"] }
```
Response `200`:
```json
{
  "indexed": [ { "url": "https://example.com/a", "lineCount": 12 } ],
  "failed":  [ { "url": "https://bad.example", "error": "timeout" } ],
  "totalRotations": 143
}
```
Per-URL failures are captured in `failed[]`; they do not fail the whole request.
Re-indexing a URL replaces its rows (delete-by-URL then insert) → idempotent.

### `GET /api/search?q=<keyword>`
Response `200`:
```json
{
  "query": "soft",
  "resultCount": 5,
  "groups": [
    { "url": "https://example.com/b",
      "contexts": ["software architecture is the process of ...",
                   "software engineering is the process of ..."] }
  ]
}
```
Blank/missing `q` → `400`.

Existing `POST/DELETE /api/kwic` and `/api/health`, `/api/db-health` are left in place.

## Data model (MySQL)

JPA entity `KwicLine`:

| column       | type          | notes                                             |
|--------------|---------------|---------------------------------------------------|
| `id`         | BIGINT PK auto| generated                                         |
| `uri`        | VARCHAR(2048) | indexed; source URL                               |
| `first_word` | VARCHAR(255)  | **indexed**; leading word of the rotation         |
| `line_text`  | VARCHAR(1024) | full rotation text = keyword-in-context           |

Spring Data `KwicLineRepository`:
- `List<KwicLine> findByFirstWordStartingWithIgnoreCaseOrderByFirstWordAsc(String prefix)`
- `void deleteByUriIn(Collection<String> uris)`

This closes the `OutputBuffer.writeToDatabase()` TODO.

## Backend components (new/changed)

- **`UrlFetcher`** (new) — `java.net.http.HttpClient`, ~10s timeout, follows redirects,
  returns raw body + content-type. Applies the SSRF guard (scheme + private-IP block)
  before any request.
- **`HtmlTextExtractor`** (new) — Jsoup: parse HTML, drop `script`/`style`, select
  block-level elements (`title`, `h1-h6`, `p`, `li`) and return each element's text as one
  "line". Non-HTML content types are skipped.
- **`InputBuffer`** (changed) — read `{text, uri}` JSONL records (or accept pre-built
  (text, uri) pairs); carry the URI through the queue so workers can call
  `LineStorage.storeLine(text, uri)`.
- **`KwicIndexService`** (new) — orchestrates fetch → extract → KWIC → persist; builds the
  per-URL summary; enforces caps.
- **`SearchController`** / extend `KwicController` (new endpoints) — `POST /api/index`,
  `GET /api/search`; grouping by URL into the response DTO.
- **`OutputBuffer.writeToDatabase()`** (filled) — persist merged rotations via the repository.

## Frontend components (new/changed)

Reuse the existing sage / Space-Grotesk design system and `ResultPanel` conventions.

- **Build Index panel** — textarea of URLs (one per line) + "Build index" button; renders a
  per-URL status list (✓ indexed with line count / ✗ error message).
- **Search bar** — prominent input + "Search" button (the new centerpiece); calls
  `GET /api/search`.
- **`SearchResults.jsx`** (new) — results grouped by URL: a clickable URL heading with its
  matching context lines underneath, keyword highlighted in each line.
- Remove `KwicResultRow` usage (incremental view) from the main flow.

## Error handling & limits

- Per-URL fetch timeout (~10s); fetch/parse failures → `failed[]`, other URLs continue.
- Caps to bound rotation explosion / DB size: max URLs/request (~20), max page bytes,
  max lines/doc, max words/line (over-long lines skipped).
- Non-HTML / unsupported content-type → skipped with a reason.
- SSRF guard rejects non-http(s) schemes and localhost/private IP targets → recorded as
  a failed URL, not a crash.
- Search: blank `q` → `400`; no matches → `200` with empty `groups` (UI shows "no results").
- DB errors → `500` with a safe message.

## Test plan

Backend (JUnit, existing `KwicSystemTest` style):
- `HtmlTextExtractor`: tags stripped, script/style removed, block elements → lines.
- `UrlFetcher`: SSRF guard rejects localhost/private/non-http(s); timeout/error handling (mocked).
- Index→persist: rotations stored with correct `first_word` + `uri`; re-index replaces rows.
- Search: prefix + case-insensitive matching; grouping by URL; empty-result case.
- MVC: `POST /api/index` (fetcher mocked), `GET /api/search` JSON shape + validation 400s.

Frontend (component/manual):
- Build-index flow shows per-URL ✓/✗ statuses.
- Search renders grouped results with highlighted keyword.
- Empty query, no-results, and backend-unavailable states.

## Dependency note (per global policy)

New dependency: **`org.jsoup:jsoup`**. Justification: the JDK ships no HTML parser; regex
tag-stripping is unreliable and would pollute the KWIC index with `script`/`style`/nav markup
and cannot cleanly segment a page into block-level lines. Jsoup is the standard, lightweight
tool for exactly this and is the single biggest quality lever for indexing real web pages.
HTTP fetching uses the stdlib `java.net.http.HttpClient` (no dependency).
