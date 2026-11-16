import { useState, useEffect } from "react";
import "../styles.css";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const MAX_QUERY_LENGTH = 500;

const RESULTS_PER_PAGE_OPTIONS = [5, 10, 20, 50, 100];

export default function SearchInterface({
    ResultComponent,
    getResultProps,
    onResultsLoaded,
    onSearchStarted,
    refreshTrigger,
}) {
    const [text, setText] = useState("");
    const [results, setResults] = useState([]);
    const [error, setError] = useState("");
    const [isSearching, setIsSearching] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);

    const [isCaseSensitive, setIsCaseSensitive] = useState(false);
    const [rankingStrategy, setRankingStrategy] = useState("title");

    const [resultsPerPage, setResultsPerPage] = useState(10);
    const [currentPage, setCurrentPage] = useState(1);

    const getSearchResults = async (event) => {
        event?.preventDefault();

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

        onSearchStarted?.();


        setError("");
        setHasSearched(false);
        setIsSearching(true);

        try {
            const params = new URLSearchParams({
                query,
                isCaseSensitive: String(isCaseSensitive),
                rankingStrategy
            });


            const response = await fetch(
                `${API_BASE_URL}/api/search?${params.toString()}`
            );

            if (!response.ok) {
                throw new Error(
                    `Backend returned HTTP ${response.status}`
                );
            }

            const data = await response.json();

            if (!Array.isArray(data)) {
                throw new Error(
                    "The backend returned an unexpected response format."
                );
            }

            console.log(data);

            setResults(data);
            if (event) {
                setCurrentPage(1);
            }
            onResultsLoaded?.(data);

            setHasSearched(true);
        }
        catch (err) {
            console.error("Unable to search:", err);

            setError(
                "Couldn't retrieve search results. Check the backend connection and try again."
            );
            setResults([]);
        }
        finally {
            setIsSearching(false);
        }
    }
    
    const totalPages = Math.max(1, Math.ceil(results.length / resultsPerPage));
    const page = Math.min(currentPage, totalPages);
    const startIndex = (page - 1) * resultsPerPage;
    const pageResults = results.slice(startIndex, startIndex + resultsPerPage);

    const goToPage = (newPage) => {
        setCurrentPage(newPage);
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    useEffect(() => {
        if (refreshTrigger > 0) {
            getSearchResults();
        }
    }, [refreshTrigger]);

    return (
        <main id="main">
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

                    <div className="ranking-option">
                        <label htmlFor="results-per-page">
                            Results per page
                        </label>

                        <select
                            id="results-per-page"
                            value={resultsPerPage}
                            onChange={(event) => {
                                setResultsPerPage(Number(event.target.value));
                                setCurrentPage(1);
                            }}
                        >
                            {RESULTS_PER_PAGE_OPTIONS.map((option) => (
                                <option key={option} value={option}>
                                    {option}
                                </option>
                            ))}
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
                {pageResults.map((result) => (
                    <ResultComponent
                        key={result.url}
                        title={result.title}
                        url={result.url}
                        description={result.description}
                        {...(getResultProps?.(result) ?? {})}
                    />
                ))}
                        </ul>

            {results.length > 0 && !isSearching && (
                <nav className="pagination" aria-label="Search results pages">
                    <button
                        type="button"
                        className="pagination-button"
                        onClick={() => goToPage(page - 1)}
                        disabled={page <= 1}
                        aria-label="Previous page"
                    >
                        &larr;
                    </button>

                    <span className="pagination-info">
                        Page {page} of {totalPages}
                    </span>

                    <button
                        type="button"
                        className="pagination-button"
                        onClick={() => goToPage(page + 1)}
                        disabled={page >= totalPages}
                        aria-label="Next page"
                    >
                        &rarr;
                    </button>
                </nav>
            )}
        </main>
    );
}