import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";
import "./styles.css";
import ResultPanel from "./ResultPanel";
import KwicResultRow from "./KwicResultRow";

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

export default function KWIC2() {
  const [text, setText] = useState("");
  const [result, setResult] = useState(null);
  const [incrementalResults, setIncrementalResults] = useState([]);
  const [error, setError] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
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

  function updateText(value) {
    setText(value);
    setError("");
  }

  async function generateIndex(event) {
    event.preventDefault();

    if (isProcessing) {
      return;
    }

    const validationError = validateInput(text);

    if (validationError) {
      setError(validationError);
      return;
    }

    // Clear previous results before starting a new request.
    setResult(null);
    setIncrementalResults([]);
    setError("");
    setIsProcessing(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/kwic`,
        {
          method: "POST",
          headers:
          {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ text })
        });

      if (!response.ok) {
        throw new Error(`Backend returned HTTP ${response.status}`);
      }

      const results = await response.json();

      console.log("KWIC backend response:", results);

      /*
       * Main KWIC output.
       *
       * The backend now returns the complete merged output
       * as a newline-separated string.
       */
      const mergedLines = String(results.mergedOutput ?? "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);

      setResult(
        {
          lines: mergedLines,
          shiftCount: mergedLines.length,
          inputLineCount: text
            .split(/\r?\n/)
            .filter((line) => line.trim())
            .length
        }
      );

      /*
       * Incremental output.
       *
       * Each element in incrementalOutput becomes
       * exactly one KwicResultRow.
       */
      const incremental = Array.isArray(results.incrementalOutput)
        ? results.incrementalOutput
        : [];

      setIncrementalResults(incremental);

      setBackendStatus("Backend connected");
    }
    catch (err) {
      console.error("Unable to generate KWIC index:", err);

      setBackendStatus("Backend unavailable");

      setError(
        "Couldn't reach the KWIC backend. Check that it is running and try again."
      );
    }
    finally {
      setIsProcessing(false);
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main">
          <span className="brand-icon" aria-hidden="true">
            K
          </span>
          KWIC
          <span className="brand-detail">
            / Text lab
          </span>
        </a>

        <span
          className={`connection ${backendStatus === "Backend connected" ? "connected" : ""
            }`}
        >
          <span className="status-dot" aria-hidden="true" />
          {backendStatus}
        </span>
      </header>

      <main id="main">
        <section className="intro" aria-labelledby="page-title">
          <p className="eyebrow">
            KEY WORD IN CONTEXT
          </p>

          <h1 id="page-title">
            Every word.
            <br />
            <span>In context.</span>
          </h1>
        </section>

        <div className="workspace">

          {/* Top row */}
          <div className="workspace-top">

            {/* Input panel */}
            <section
              className="panel input-panel"
              aria-labelledby="input-heading"
            >
              <div className="panel-heading">
                <h2 id="input-heading">
                  Your Text
                </h2>

                <span className="tag">
                  INPUT
                </span>
              </div>

              <form onSubmit={generateIndex}>
                <label htmlFor="kwic-input">
                  Input text
                </label>

                <p id="input-help" className="help">
                  Enter one phrase or sentence per line.
                  Blank lines are ignored.
                </p>

                <textarea
                  id="kwic-input"
                  value={text}
                  onChange={(event) =>
                    updateText(event.target.value)
                  }
                  placeholder={
                    "software architecture project\nkey word in context"
                  }
                  maxLength={10000}
                  disabled={isProcessing}
                  aria-describedby="input-help input-limits"
                  spellCheck="false"
                />

                <div className="input-meta">
                  <span>
                    {text.length.toLocaleString()} / 10,000
                  </span>
                </div>

                <p id="input-limits" className="limits">
                  Up to 100 lines · 50 words per line · 1,000 words total
                </p>

                {error && (
                  <p className="error" role="alert">
                    {error}
                  </p>
                )}

                <div className="actions">
                  <button
                    className="primary-button"
                    type="submit"
                    disabled={!text.trim() || isProcessing}
                  >
                    {isProcessing
                      ? "Generating…"
                      : "Generate index"}

                    <span aria-hidden="true">
                      {" "}→
                    </span>
                  </button>

                  <button
                    className="secondary-button"
                    type="button"
                    disabled={!text || isProcessing}
                    onClick={() => {
                      updateText("");
                      setResult(null);
                      setIncrementalResults([]);
                    }}
                  >
                    Clear
                  </button>
                </div>
              </form>
            </section>

            {/* Main KWIC output */}
            <ResultPanel
              title="KWIC Index"
              data={result}
              showResults={!isProcessing && result !== null}
              isProcessing={isProcessing}
            />
          </div>

          {/* Incremental results */}
          <section
            className="incremental-results"
            aria-labelledby="incremental-results-heading"
          >
            <div className="incremental-heading">
              <h2 id="incremental-results-heading">
                Processing Results
              </h2>

              <span className="tag">
                INCREMENTAL
              </span>
            </div>

            <div className="vertical-list">
              {incrementalResults.map((incrementalResult, index) => (
                <KwicResultRow
                  key={index}
                  lineNumber={index + 1}
                  inputLine={incrementalResult.inputLines}
                  circularShifts={incrementalResult.shiftedLines}
                  sortedAlphabetically={incrementalResult.alphabetizedLines}
                  isProcessing={isProcessing}
                />
              ))}
            </div>
          </section>

        </div>
      </main>

      <footer>
        <div>
          <strong>
            SE 6362 · Software Architecture
          </strong>

          <p>
            Kalani Kawaguchi · Zhi Li · Jonathan Loper · Mitchell Vu
          </p>
        </div>

        <a href={ProjPlan}>
          Preliminary project plan ↗
        </a>
      </footer>
    </div>
  );
}
