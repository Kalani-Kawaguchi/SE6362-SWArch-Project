import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";
import "./styles.css";

const API_BASE_URL = (process.env.API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
const EXAMPLE = "software architecture project\nkey word in context";

const MAX_LINES = 100;
const MAX_WORDS_PER_LINE = 50;
const MAX_WORDS_TOTAL = 1000;

// Returns an error message, or "" if the input is valid.
function validateInput(text) {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);
  if (lines.length === 0) return "Enter at least one line of text.";
  if (lines.length > MAX_LINES) return `Use at most ${MAX_LINES} lines (you have ${lines.length}).`;
  let total = 0;
  for (let i = 0; i < lines.length; i++) {
    const words = lines[i].split(/\s+/).length;
    if (words > MAX_WORDS_PER_LINE) return `Line ${i + 1} has ${words} words. Use at most ${MAX_WORDS_PER_LINE} per line.`;
    total += words;
  }
  if (total > MAX_WORDS_TOTAL) return `Use at most ${MAX_WORDS_TOTAL.toLocaleString()} words in total (you have ${total.toLocaleString()}).`;
  return "";
}

export default function KWIC2() {
  const [text, setText] = useState("");
  const [sortedShifts, setSortedShifts] = useState(null);
  const [circularShifts, setCircularShifts] = useState(null);
  const [alphabetizer, setAlphabetizer] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [backendStatus, setBackendStatus] = useState("Checking backend…");

  // Check whether the backend is reachable. no-cors avoids CORS failures;
  // any response (even an opaque one) means the server is up.
  useEffect(() => {
    let cancelled = false;
    fetch(API_BASE_URL, { mode: "no-cors" })
      .then(() => !cancelled && setBackendStatus("Backend connected"))
      .catch(() => !cancelled && setBackendStatus("Backend unavailable"));
    return () => { cancelled = true; };
  }, []);

  function updateText(value) {
    setText(value);
    setError("");
  }

  async function generateIndex(event) {
    event.preventDefault();
    if (isProcessing) return;

    const validationError = validateInput(text);
    if (validationError) {
      setError(validationError);
      return;
    }

    setSortedShifts("");
    setCircularShifts("");
    setAlphabetizer("");

    setError("");
    setResult(null);
    setIsProcessing(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/kwic`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
      });

      if (!response.ok) {
        throw new Error(`Backend returned HTTP ${response.status}`);
      }

      const results = await response.json();
      console.log(results)
      // "output" is the final sorted index (string or array of lines);
      // fall back to the alphabetizer stage if it is missing.
      const rawLines = results.output?? results.alphabetizer ?? [];
      console.log(rawLines)
      const lines = (Array.isArray(rawLines) ? rawLines : String(rawLines).split(/\r?\n/))
      .filter((line) => line.trim() !== "");

      let shifts = [];
      let alpha = [];
      const output = results.output;

      results.circularShift.forEach((entry) => {
      String(entry).split(/\r?\n/).forEach((line) => {
          if (line.trim() !== "") shifts.push(line.trimEnd());
        });
      });

      setCircularShifts({shifts,
      shiftCount: shifts.length,
      inputLineCount: text.split(/\r?\n/).filter((l) => l.trim()).length,
      });

      results.alphabetizer.forEach((entry) => {
      String(entry).split(/\r?\n/).forEach((line) => {
          if (line.trim() !== "") alpha.push(line.trimEnd());
        });
      });

      setAlphabetizer({alpha,
      shiftCount: alpha.length,
      inputLineCount: text.split(/\r?\n/).filter((l) => l.trim()).length,
      });
      console.log(alpha)

      setResult({
        lines,
        shiftCount: Array.isArray(results.output) ? results.circularShift.length : lines.length,
        inputLineCount: text.split(/\r?\n/).filter((l) => l.trim()).length,
      });
      setBackendStatus("Backend connected");
    } catch (err) {
      console.error("Unable to generate KWIC index:", err);
      setBackendStatus("Backend unavailable");
      setError("Couldn't reach the KWIC backend. Check that it is running and try again.");
    } finally {
      setIsProcessing(false);
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main"><span className="brand-icon" aria-hidden="true">K</span>KWIC<span className="brand-detail">/ Text lab</span></a>
        <span className={`connection ${backendStatus === "Backend connected" ? "connected" : ""}`}>
          <span className="status-dot" aria-hidden="true" />{backendStatus}
        </span>
      </header>

      <main id="main">
        <section className="intro" aria-labelledby="page-title">
          <p className="eyebrow">KEY WORD IN CONTEXT</p>
          <h1 id="page-title">Every word.<br /><span>In context.</span></h1>
        </section>

        <div className="workspace">
          <section className="panel" aria-labelledby="input-heading">
            <div className="panel-heading"><h2 id="input-heading"><span className="step">01</span>Your text</h2><span className="tag">INPUT</span></div>
            <form onSubmit={generateIndex}>
              <label htmlFor="kwic-input">Input text</label>
              <p id="input-help" className="help">Enter one phrase or sentence per line. Blank lines are ignored.</p>
              <textarea id="kwic-input" value={text} onChange={(event) => updateText(event.target.value)}
                placeholder={"software architecture project\nkey word in context"} maxLength={10000}
                disabled={isProcessing} aria-describedby="input-help input-limits" spellCheck="false" />
              <div className="input-meta"><span>{text.length.toLocaleString()} / 10,000</span></div>
              <p id="input-limits" className="limits">Up to 100 lines · 50 words per line · 1,000 words total</p>
              {error && <p className="error" role="alert">{error}</p>}
              <div className="actions">
                <button className="primary-button" type="submit" disabled={!text.trim() || isProcessing}>{isProcessing ? "Generating…" : "Generate index"}<span aria-hidden="true"> →</span></button>
                <button className="secondary-button" type="button" disabled={!text || isProcessing} onClick={() => { updateText(""); setResult(null); }}>Clear</button>
              </div>
            </form>
          </section>

          <section className="panel results-panel" aria-labelledby="results-heading" aria-busy={isProcessing}>
            <div className="panel-heading"><h2 id="results-heading"><span className="step">02</span>Your index</h2><span className="tag">A–Z</span></div>
            <p className="result-summary" role="status">{isProcessing ? "Generating your index…" : result ? `${result.shiftCount} rotations from ${result.inputLineCount} input ${result.inputLineCount === 1 ? "line" : "lines"}` : "Your results will appear here."}</p>
            {result ? <ol className="results">{result.lines.map((line, index) => <li key={index}>{line}</li>)}</ol> :
              <div className="empty-state"><div className="rotation-example" aria-hidden="true"><span>word in context</span><span>in context word</span><span>context word in</span></div><p>{isProcessing ? "Rotating words and sorting lines." : ""}</p></div>}
          </section>

          <section className="panel results-panel" aria-labelledby="results-heading" aria-busy={isProcessing}>
            <div className="panel-heading"><h2 id="results-heading"><span className="step">02</span>Circular Shifts index</h2><span className="tag">A–Z</span></div>
            <p className="result-summary" role="status">{isProcessing ? "Generating your index…" : circularShifts ? `${circularShifts.shiftCount} rotations from ${circularShifts.inputLineCount} input ${circularShifts.inputLineCount === 1 ? "line" : "lines"}` : "Your results will appear here."}</p>
            {result ? <ol className="results">{circularShifts.shifts.map((line, index) => <li key={index}>{line}</li>)}</ol> :
              <div className="empty-state"><div className="rotation-example" aria-hidden="true"><span>word in context</span><span>in context word</span><span>context word in</span></div><p>{isProcessing ? "Rotating words and sorting lines." : ""}</p></div>}
          </section>

          <section className="panel results-panel" aria-labelledby="results-heading" aria-busy={isProcessing}>
            <div className="panel-heading"><h2 id="results-heading"><span className="step">02</span>Alphabetized index</h2><span className="tag">A–Z</span></div>
            <p className="result-summary" role="status">{isProcessing ? "Generating your index…" : alphabetizer ? `${alphabetizer.shiftCount} rotations from ${alphabetizer.inputLineCount} input ${alphabetizer.inputLineCount === 1 ? "line" : "lines"}` : "Your results will appear here."}</p>
            {result ? <ol className="results">{alphabetizer.alpha.map((line, index) => <li key={index}>{line}</li>)}</ol> :
              <div className="empty-state"><div className="rotation-example" aria-hidden="true"><span>word in context</span><span>in context word</span><span>context word in</span></div><p>{isProcessing ? "Rotating words and sorting lines." : ""}</p></div>}
          </section>
          
        </div>
      </main>

      <footer><div><strong>SE 6362 · Software Architecture</strong><p>Kalani Kawaguchi · Zhi Li · Jonathan Loper · Mitchell Vu</p></div><a href={ProjPlan}>Preliminary project plan ↗</a></footer>
    </div>
  );
}
