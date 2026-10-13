import ProjPlan from "../documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";
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


    return (


        <main id="main">
            <section className="intro" aria-labelledby="page-title">
                <p className="eyebrow">
                    SEARCH ENGINE
                </p>

                <h1 id="page-title">
                    Search Engine
                </h1>
            </section>


        </main>


    );
}
