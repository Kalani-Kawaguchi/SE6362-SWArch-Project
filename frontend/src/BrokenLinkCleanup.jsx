import { useState } from "react";

export default function BrokenLinkCleanup({
    apiBaseUrl,
    disabled,
    onDeleted
}) {
    const [results, setResults] = useState(null);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const broken = (results ?? []).filter((result) => result.status === "BROKEN");

    const unknown = (results ?? []).filter((result) => result.status === "UNKNOWN");

    const request = async (action, ids) => {
        const response = await fetch(
            `${apiBaseUrl}/api/kwic/broken-links/${action}`,
            {
                method: "POST",
                ...(ids ? {
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(ids)
                } : {})
            }
        );

        if (!reponse.ok) {
            throw new Error(
                response.status === 409
                    ? "A link check is already running. Try again shortly."
                    : `The request failed (HTTP ${response.status}).`
            );
        }

        return response.json();
    };

    const scan = async () => {
        setBusy(true);
        setError("");
        setMessage("");
        setResults(null);

        try {
            const data = await request("scan");

            if (!Array.isArray(data)) {
                throw new Error("Unexpected scan response.");
            }

            setResults(data);
        } catch (error) {
            setError(error.message);
        } finally {
            setBusy(false);
        }
    };

    const deleteBroken = async () => {
        if (!window.confirm(
            `Recheck and delete all ${broken.length} broken links? This cannot be undone.`
        )) {
            return;
        }

        setBusy(true);
        setError("");
        setMessage("");

        try {
            const data = await request("delete", broken.map((result) => result.id));

            setMessage(`Deleted ${data.deleted} broken links. Kept ${data.kept} entries after rechecking.`);

            onDeleted();
        } catch (error) {
            setError(`${error.message} Run another scan before retrying.`);
        } finally {
            setResults(null);
            setBusy(false);
        }
    };

    return (
        <section aria-labelledby="broken-links-heading" aria-busy={busy}>
            <h2 id="broken-links-heading">Broken Links</h2>

            <button type="button"
                    onClick={scan}
                    disabled={disabled || busy}
            >
                    {busy ? "Checking links..." : "Check for broken links"}
            </button>

            {disabled && (
                <p>Save or cancel your manual changes before checking links.</p>
            )}

            {results !== null && (
                <>
                    <p role="status">
                        {results.length} entries reviewed · {" "}
                        {broken.length} broken · {" "}
                        {unknown.length} inconclusive
                    </p>

                    {broken.length > 0 && (
                        <>
                            <ul>
                                {broken.map((result) => (
                                    <li key={result.id}>
                                        {result.url} - {result.reason}
                                    </li>
                                ))}
                            </ul>

                            <button
                                type="button"
                                onClick={deleteBroken}
                                disabled={disabled || busy}
                            >
                                Delete all confirmed broken links
                            </button>
                        </>
                    )}

                    {unknown.length > 0 && (
                        <details>
                            <summary>Links kept for manual review</summary>
                            <ul>
                                {unknown.map((result) => (
                                    <li key={result.id}>
                                        {result.url} - {result.reason}
                                    </li>
                                ))}
                            </ul>
                        </details>
                    )}
                </>
            )}

            {message && <p role="status">{message}</p>}
            {error && <p role="alert">{error}</p>}
        </section>
    );
}