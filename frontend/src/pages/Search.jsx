import ProjPlan from "../documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";
import SearchResult from "../SearchResult";
import "../styles.css";

const API_BASE_URL = (process.env.API_BASE_URL || "http://localhost:8080")
    .replace(/\/+$/, "");
const MAX_LINES = 100;
const MAX_WORDS_PER_LINE = 50;
const MAX_WORDS_TOTAL = 1000;

// Returns an error message, or "" if the input is valid.
function validateInput(text) {
    const lines = text
        .split(/\r?\n/)
        .map((l) => l.trim())
        .filter(Boolean);

    if (lines.length === 0) {
        return "Enter at least one line of text.";
    }

    if (lines.length > MAX_LINES) {
        return `Use at most ${MAX_LINES} lines (you have ${lines.length}).`;
    }

    let total = 0;

    for (let i = 0; i < lines.length; i++) {
        const words = lines[i].split(/\s+/).length;

        if (words > MAX_WORDS_PER_LINE) {
            return `Line ${i + 1} has ${words} words. Use at most ${MAX_WORDS_PER_LINE} per line.`;
        }

        total += words;
    }

    if (total > MAX_WORDS_TOTAL) {
        return `Use at most ${MAX_WORDS_TOTAL.toLocaleString()} words in total (you have ${total.toLocaleString()}).`;
    }

    return "";
}

// Several shifted lines can come from the same page, so group them by URL.
function groupByUrl(results) {
    const groups = new Map();

    for (const result of results) {
        const key = result.url ?? "(no URL)";
        if (!groups.has(key)) {
            groups.set(key, []);
        }
        groups.get(key).push(result.context);
    }

    return Array.from(groups, ([url, contexts]) => ({ url, contexts }));
}

function hostnameOf(url) {
    try {
        return new URL(url).hostname;
    } catch {
        return url;
    }
}

export default function Search() {
    const [backendStatus, setBackendStatus] = useState("Checking backend…");

    // Check whether the backend is reachable.
    useEffect(() => {
        let cancelled = false;

        fetch(API_BASE_URL, { mode: "no-cors" })
            .then(() => {
                if (!cancelled) {
                    setBackendStatus("Backend connected");
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setBackendStatus("Backend unavailable");
                }
            });

        return () => {
            cancelled = true;
        };
    }, []);

    const [query, setQuery] = useState("");
    const [results, setResults] = useState(null);
    const [error, setError] = useState("");
    const [isSearching, setIsSearching] = useState(false);

    async function runSearch(event) {
        event.preventDefault();

        const q = query.trim();
        if (!q) {
            setError("Enter a keyword to search.");
            return;
        }

        setError("");
        setIsSearching(true);

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/search?q=${encodeURIComponent(q)}`
            );

            if (!response.ok) {
                throw new Error(`Backend returned HTTP ${response.status}`);
            }

            const data = await response.json();
            setResults(groupByUrl(data));
        } catch (err) {
            setResults(null);
            setError(err.message);
        } finally {
            setIsSearching(false);
        }
    }


    return (


        <main id="main">
            <section className="intro" aria-labelledby="page-title">
                <p className="eyebrow">
                    SEARCH ENGINE
                </p>

                <h1 id="page-title">
                    <span>Search</span> the Web
                </h1>
            </section>

            <form className="search-form" role="search" onSubmit={runSearch}>
                <label className="sr-only" htmlFor="search-input">
                    Search the web
                </label>

                <input
                    id="search-input"
                    className="search-input"
                    type="search"
                    placeholder="Enter your search query..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                />

                <button className="search-button" type="submit" disabled={isSearching}>
                    {isSearching ? "Searching…" : "Search"}
                </button>
            </form>

            {error && <p className="error">{error}</p>}

            {results === null && (
                <ul className="search-results-list">
                    <SearchResult
                        title="Google"
                        url="http://google.com"
                        description="Google is a world famous search engine."
                    />
                    <SearchResult
                        title="Facebook"
                        url="http://facebook.com"
                        description="Connect with Friends and Family."
                    />
                </ul>
            )}

            {results !== null && (
                <ul className="search-results-list">
                    {results.length === 0 && (
                        <li className="search-result">No pages contain that keyword.</li>
                    )}

                    {results.map(({ url, contexts }) => (
                        <SearchResult
                            key={url}
                            title={hostnameOf(url)}
                            url={url}
                            description={contexts.map((context, i) => {
                                const [keyword, ...rest] = context.split(" ");
                                return (
                                    <span key={i} style={{ display: "block" }}>
                                        <strong>{keyword}</strong> {rest.join(" ")}
                                    </span>
                                );
                            })}
                        />
                    ))}
                </ul>
            )}
        </main>
    );
}