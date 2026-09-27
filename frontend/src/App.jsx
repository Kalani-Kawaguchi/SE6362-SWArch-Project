import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";
import "./styles.css";

const API_BASE_URL = (process.env.API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
const EXAMPLE = "software architecture project\nkey word in context";

export default function App() {
  const [backendStatus, setBackendStatus] = useState("Checking connection");
  const [text, setText] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 10000);
    fetch(`${API_BASE_URL}/api/health`, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error("Health check failed");
        if (active) setBackendStatus("Backend connected");
      })
      .catch(() => {
        if (active) setBackendStatus("Backend unavailable");
      })
      .finally(() => clearTimeout(timer));
    return () => {
      active = false;
      clearTimeout(timer);
      controller.abort();
    };
  }, []);

  function updateText(value) {
    setText(value);
    setResult(null);
    setError("");
  }

  async function generateIndex(event) {
    event.preventDefault();
    if (!text.trim() || isProcessing) return;
    setIsProcessing(true);
    setError("");
    setResult(null);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch(`${API_BASE_URL}/api/kwic`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
        signal: controller.signal,
      });
      const data = await response.json().catch(() => null);
      if (!response.ok) {
        setError(response.status === 400 && typeof data?.message === "string"
          ? data.message : "Unable to generate the index. Please try again.");
        return;
      }
      if (!Array.isArray(data?.lines) || !data.lines.every((line) => typeof line === "string")) {
        setError("The server returned an unexpected response. Please try again.");
        return;
      }
      setResult(data);
      setBackendStatus("Backend connected");
    } catch (requestError) {
      setError(requestError.name === "AbortError"
        ? "The request timed out. Please try again."
        : "Unable to connect to the backend. Check your connection and try again.");
    } finally {
      clearTimeout(timer);
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
          <p className="intro-copy">Turn a few lines of text into an alphabetical index. Each word takes a turn at the beginning, with the rest of its line kept in context.</p>
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
              <div className="input-meta"><button className="text-button" type="button" disabled={isProcessing} onClick={() => updateText(EXAMPLE)}>Try an example</button><span>{text.length.toLocaleString()} / 10,000</span></div>
              <p id="input-limits" className="limits">Up to 100 lines · 50 words per line · 1,000 words total</p>
              {error && <p className="error" role="alert">{error}</p>}
              <div className="actions">
                <button className="primary-button" type="submit" disabled={!text.trim() || isProcessing}>{isProcessing ? "Generating…" : "Generate index"}<span aria-hidden="true"> →</span></button>
                <button className="secondary-button" type="button" disabled={!text || isProcessing} onClick={() => updateText("")}>Clear</button>
              </div>
            </form>
          </section>

          <section className="panel results-panel" aria-labelledby="results-heading" aria-busy={isProcessing}>
            <div className="panel-heading"><h2 id="results-heading"><span className="step">02</span>Your index</h2><span className="tag">A–Z</span></div>
            <p className="result-summary" role="status">{isProcessing ? "Generating your index…" : result ? `${result.shiftCount} rotations from ${result.inputLineCount} input ${result.inputLineCount === 1 ? "line" : "lines"}` : "Your results will appear here."}</p>
            {result ? <ol className="results">{result.lines.map((line, index) => <li key={index}>{line}</li>)}</ol> :
              <div className="empty-state"><div className="rotation-example" aria-hidden="true"><span>word in context</span><span>in context word</span><span>context word in</span></div><p>{isProcessing ? "Rotating words and sorting lines." : "Add your text, then generate an index to see every circular shift in alphabetical order."}</p></div>}
            <p className="result-note">Sorted without regard to capitalization. Original spelling and punctuation are preserved.</p>
          </section>
        </div>
        <p className="privacy-note">This web flow processes text in memory. Results are not saved to a database.</p>
      </main>

      <footer><div><strong>SE 6362 · Software Architecture</strong><p>Kalani Kawaguchi · Zhi Li · Jonathan Loper · Mitchell Vu</p></div><a href={ProjPlan}>Preliminary project plan ↗</a></footer>
    </div>
  );
}
