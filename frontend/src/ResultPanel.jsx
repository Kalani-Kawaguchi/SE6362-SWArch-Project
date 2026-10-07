export default function ResultPanel({
    title,
    data,
    showResults,
    isProcessing,
    emptyMessage = "Rotating words and sorting lines.",
}) {
    const lines = data?.lines || [];

    return (
        <section
            className="panel results-panel"
            aria-labelledby={`${title}-heading`}
            aria-busy={isProcessing}
        >
            <div className="panel-heading">
                <h2 id={`${title}-heading`}>
                    {title}
                </h2>

                <span className="tag">A–Z</span>
            </div>

            <p className="result-summary" role="status">
                {isProcessing
                    ? "Generating your index…"
                    : data
                        ? `${data.shiftCount} rotations from ${data.inputLineCount} input ${data.inputLineCount === 1 ? "line" : "lines"
                        }`
                        : "Your results will appear here."}
            </p>

            {showResults && data ? (
                <ol className="results">
                    {lines.map((line, index) => (
                        <li key={index}>{line}</li>
                    ))}
                </ol>
            ) : (
                <div className="empty-state">
                    <div className="rotation-example" aria-hidden="true">
                        <span>word in context</span>
                        <span>in context word</span>
                        <span>context word in</span>
                    </div>

                    <p>
                        {isProcessing ? emptyMessage : ""}
                    </p>
                </div>
            )}
        </section>
    );
}

