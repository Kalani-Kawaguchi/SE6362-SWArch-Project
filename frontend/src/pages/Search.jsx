/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: Search.jsx
    Description: React component for search engine
*/

import { useState } from "react";
import SearchResult from "../SearchResult";
import "../styles.css";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const MAX_QUERY_LENGTH = 500;

export default function Search() {
    const [text, setText] = useState("");
    const [results, setResults] = useState([]);
    const [error, setError] = useState("");
    const [isSearching, setIsSearching] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);

    const [isCaseSensitive, setIsCaseSensitive] = useState(false);
    const [rankingStrategy, setRankingStrategy] = useState("title");

    // Get results of keyword search by calling backend API
    async function getSearchResults(event) {
        event.preventDefault();

        if (isSearching) {
            return;
        }

        const query = text.trim();

        if (!query) {
            setError("Enter a search query.");
            return;
        }

        if (query.length > MAX_QUERY_LENGTH) {
            setError(
                `Search queries must be at most ${MAX_QUERY_LENGTH} characters.`
            );
            return;
        }

        setResults([]);
        setError("");
        setHasSearched(false);
        setIsSearching(true);

        try {
            const params = new URLSearchParams(
                {
                    query,
                    isCaseSensitive: String(isCaseSensitive),
                    rankingStrategy
                });

            const response = await fetch(
                `${API_BASE_URL}/api/search?${params.toString()}`,
                {
                    method: "GET"
                }
            );

            if (!response.ok) {
                throw new Error(
                    `Backend returned HTTP ${response.status}`
                );
            }

            const responseText = await response.text();

            console.log("Raw API response:", responseText);

            const data = JSON.parse(responseText);

            console.log("Parsed response:", data);
            console.log("Is array:", Array.isArray(data));

            if (!Array.isArray(data)) {
                throw new Error(
                    "The backend returned an unexpected response format."
                );
            }

            setResults(data);
            setHasSearched(true);
        }
        catch (err) {
            console.error("Unable to search:", err);

            setError(
                "Couldn't retrieve search results. Check the backend connection and try again."
            );
        }
        finally {
            setIsSearching(false);
        }
    }

    return (
        <main id="main">
            <section
                className="intro"
                aria-labelledby="page-title"
            >
                <p className="eyebrow">
                    SEARCH ENGINE
                </p>

                <h1 id="page-title">
                    <span>Search</span> the Web
                </h1>
            </section>


            <form
                className="search-layout"
                role="search"
                onSubmit={getSearchResults}
            >
                <div className="search-form">
                    <label
                        className="sr-only"
                        htmlFor="search-input"
                    >
                        Search the web
                    </label>

                    <input
                        id="search-input"
                        className="search-input"
                        type="search"
                        value={text}
                        onChange={(event) => {
                            setText(event.target.value);
                            setError("");
                        }}
                        placeholder="Enter your search query..."
                        maxLength={MAX_QUERY_LENGTH}
                        disabled={isSearching}
                    />

                    <button
                        className="search-button"
                        type="submit"
                        disabled={isSearching}
                    >
                        {isSearching ? "Searching..." : "Search"}
                    </button>
                </div>

                <div className="search-options">
                    <label className="checkbox-option">
                        <input
                            type="checkbox"
                            checked={isCaseSensitive}
                            onChange={(event) =>
                                setIsCaseSensitive(event.target.checked)
                            }
                            disabled={isSearching}
                        />
                        <span>Case-sensitive search</span>
                    </label>

                    <div className="ranking-option">
                        <label htmlFor="ranking-strategy">
                            Ranking
                        </label>

                        <select
                            id="ranking-strategy"
                            value={rankingStrategy}
                            onChange={(event) =>
                                setRankingStrategy(event.target.value)
                            }
                            disabled={isSearching}
                        >
                            <option value="title">
                                Title
                            </option>

                            <option value="access_frequency">
                                Access Frequency
                            </option>
                        </select>
                    </div>
                </div>
            </form>


            {error && (
                <p className="error" role="alert">
                    {error}
                </p>
            )}

            {isSearching && (
                <p role="status">
                    Searching the web...
                </p>
            )}

            {hasSearched && !isSearching && results.length === 0 && (
                <p role="status">
                    No results found.
                </p>
            )}

            <ul className="search-results-list">
                {results.map((result, index) => (
                    <SearchResult
                        key={index}
                        title={result.title}
                        url={result.url}
                        description={result.description}
                    />
                ))}
            </ul>
        </main>
    );
}