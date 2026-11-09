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
import AddWebPages from "../AddWebPages";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

const MAX_QUERY_LENGTH = 500;

export default function Admin() {
    const [text, setText] = useState("");
    const [results, setResults] = useState([]);
    const [error, setError] = useState("");
    const [isSearching, setIsSearching] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);

    // Get results of keyword search by calling backend API
    async function getSearchResults(event) {
        event.preventDefault();

        if (isSearching) {
            return;
        }

        const query = text.trim().split(/\s+/);

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
            const response = await fetch(
                `${API_BASE_URL}/api/search`,
                {
                    method: "POST",
                    headers:
                    {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ query })
                }
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
            <AddWebPages></AddWebPages>
        </main>
    );
}
