import { useState, useEffect, useMemo, useRef, useId } from "react";
import "../styles/styles.css";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const MAX_QUERY_LENGTH = 500;

const RESULTS_PER_PAGE_OPTIONS = [5, 10, 20, 50, 100];



function getSuggestionRange(text, caret) {
    let start = caret;
    let end = caret;

    while (start > 0 && !/[\s()]/.test(text[start - 1])) {
        start--;
    }

    while (end < text.length && !/[\s()]/.test(text[end])) {
        end++;
    }

    return { start, end, term: text.slice(start, end) };
}


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

    const searchInputRef = useRef(null);
    const suggestionListId = useId();

    const [caret, setCaret] = useState(0);
    const [suggestionsOpen, setSuggestionsOpen] = useState(false);
    const [activeSuggestion, setActiveSuggestion] = useState(-1);
    const [suggestionResponse, setSuggestionResponse] = useState({
        term: "",
        caseSensitive: false,
        words: []
    });

    const suggestionRange = getSuggestionRange(text, caret);
    const suggestionTerm = suggestionRange.term;

    useEffect(() => {
        setActiveSuggestion(-1);
        setSuggestionResponse({
            term: "",
            caseSensitive: false,
            words: []
        });

        if (
            !suggestionsOpen ||
            isSearching ||
            suggestionTerm.length < 2 ||
            suggestionTerm.length > 50 ||
            ["AND", "OR", "NOT"].includes(suggestionTerm)
        ) {
            return;
        }

        const controller = new AbortController();

        const timer = setTimeout(async () => {
            try {
                const params = new URLSearchParams({
                    term: suggestionTerm,
                    isCaseSensitive: String(isCaseSensitive)
                });

                const response = await fetch(
                    `${API_BASE_URL}/api/search/suggestions?${params}`,
                    { signal: controller.signal }
                );

                if (!response.ok) {
                    return;
                }

                const words = await response.json();

                if (
                    !controller.signal.aborted &&
                    Array.isArray(words) &&
                    words.every((word) => typeof word === "string")
                ) {
                    setSuggestionResponse({
                        term: suggestionTerm,
                        caseSensitive: isCaseSensitive,
                        words
                    });
                }
            } catch {
                // Normal searching remains available if suggestions fail.
            }
        }, 200);

        return () => {
            clearTimeout(timer);
            controller.abort();
        };
    }, [suggestionTerm, isCaseSensitive, suggestionsOpen, isSearching]);

    const suggestions =
        suggestionResponse.term === suggestionTerm &&
            suggestionResponse.caseSensitive === isCaseSensitive
            ? suggestionResponse.words.filter((word) =>
                text.length -
                (suggestionRange.end - suggestionRange.start) +
                word.length <= MAX_QUERY_LENGTH)
            : [];

    const showSuggestions =
        suggestionsOpen && !isSearching && suggestions.length > 0;

    const chooseSuggestion = (word) => {
        const { start, end } = suggestionRange;
        const nextCaret = start + word.length;

        setText(text.slice(0, start) + word + text.slice(end));
        setCaret(nextCaret);
        setError("");
        setSuggestionsOpen(false);
        setActiveSuggestion(-1);

        requestAnimationFrame(() => {
            searchInputRef.current?.setSelectionRange(nextCaret, nextCaret);
        });
    };

    const handleSuggestionKeys = (event) => {
        if (event.nativeEvent.isComposing) {
            return;
        }

        if (event.key === "Escape") {
            if (suggestionsOpen) {
                event.preventDefault();
            }

            setSuggestionsOpen(false);
            return;
        }

        if (!showSuggestions) {
            return;
        }

        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();

            setActiveSuggestion((previous) => {
                if (event.key === "ArrowDown") {
                    return (previous + 1) % suggestions.length;
                }

                return previous <= 0 ? suggestions.length - 1 : previous - 1;
            });
        } else if (
            event.key === "Enter" &&
            activeSuggestion >= 0 &&
            activeSuggestion < suggestions.length
        ) {
            event.preventDefault();
            chooseSuggestion(suggestions[activeSuggestion]);
        }
    };

    const getSearchResults = async (event) => {
        event?.preventDefault();

        if (isSearching) {
            return;
        }

        const query = text
            .replace(/\s+/g, " ")
            .trim();

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

        setSuggestionsOpen(false);
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
                const errorData = await response.json();

                throw new Error(
                    errorData.error || "An unexpected error occurred."
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
                err.message || "Couldn't retrieve search results."
            );

            setResults([]);
        }
        finally {
            setIsSearching(false);
        }
    };

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
                <div className="search-form">
                    <label
                        className="sr-only"
                        htmlFor="search-input"
                    >
                        Search the web
                    </label>

                    {/* <input
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
                    /> */}

                    <div className="search-autocomplete">
                        <input
                            ref={searchInputRef}
                            id="search-input"
                            className="search-input"
                            type="search"
                            role="combobox"
                            aria-autocomplete="list"
                            aria-expanded={showSuggestions}
                            aria-controls={suggestionListId}
                            aria-activedescendant={
                                showSuggestions &&
                                    activeSuggestion >= 0 &&
                                    activeSuggestion < suggestions.length
                                    ? `${suggestionListId}-${activeSuggestion}`
                                    : undefined
                            }
                            autoComplete="off"
                            spellCheck={false}
                            value={text}
                            placeholder="Enter your search query..."
                            maxLength={MAX_QUERY_LENGTH}
                            disabled={isSearching}
                            onFocus={(event) => {
                                setCaret(event.target.selectionStart ?? text.length);
                                setSuggestionsOpen(true);
                            }}
                            onBlur={() => setSuggestionsOpen(false)}
                            onSelect={(event) => {
                                setCaret(event.target.selectionStart ?? text.length);
                                setActiveSuggestion(-1);

                                if (
                                    event.target.selectionStart !==
                                    event.target.selectionEnd
                                ) {
                                    setSuggestionsOpen(false);
                                }
                            }}
                            onChange={(event) => {
                                setText(event.target.value);
                                setCaret(
                                    event.target.selectionStart ?? event.target.value.length
                                );
                                setError("");
                                setActiveSuggestion(-1);
                                setSuggestionsOpen(true);
                            }}
                            onKeyDown={handleSuggestionKeys}
                        />

                        <ul
                            id={suggestionListId}
                            className="search-suggestions"
                            role="listbox"
                            aria-label="Search suggestions"
                            hidden={!showSuggestions}
                        >
                            {suggestions.map((word, index) => (
                                <li
                                    id={`${suggestionListId}-${index}`}
                                    key={word}
                                    role="option"
                                    aria-selected={index === activeSuggestion}
                                    onMouseDown={(event) => event.preventDefault()}
                                    onClick={() => chooseSuggestion(word)}
                                >
                                    {word}
                                </li>
                            ))}
                        </ul>
                    </div>

                    <button
                        className="search-button"
                        type="submit"
                        disabled={isSearching}
                    >
                        {isSearching ? "Searching..." : "Search"}
                    </button>
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
