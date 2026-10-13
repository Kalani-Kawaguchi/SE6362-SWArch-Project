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
            placeholder="Search a keyword (e.g. keyword, context, index)"
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
            <p className="help">
              Add one URL per line, then press Build index. Search only looks inside
              pages you’ve indexed here — it doesn’t crawl the web. The examples below
              are just placeholders; the box starts empty.
            </p>
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
