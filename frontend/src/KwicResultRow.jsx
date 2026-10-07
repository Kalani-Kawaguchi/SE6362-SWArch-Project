import ResultPanel from "./ResultPanel";

export default function KwicResultRow({
    lineNumber,
    inputLine,
    circularShifts,
    sortedAlphabetically,
    isProcessing,
}) {
    const shiftedData = circularShifts
        ? {
            lines: circularShifts
                .split(/\r?\n/)
                .filter((line) => line.trim() !== ""),
        }
        : null;

    const alphabetizedData = sortedAlphabetically
        ? {
            lines: sortedAlphabetically
                .split(/\r?\n/)
                .filter((line) => line.trim() !== ""),
        }
        : null;

    return (
        <section className="kwic-result-row">
            <h3 className="kwic-result-label">
                Line {lineNumber}: <span>{inputLine}</span>
            </h3>

            <div className="kwic-result-panels">
                <ResultPanel
                    title="Circular Shifts"
                    data={shiftedData}
                    showResults={!isProcessing && shiftedData !== null}
                    isProcessing={isProcessing}
                />

                <ResultPanel
                    title="Sorted Alphabetically"
                    data={alphabetizedData}
                    showResults={
                        !isProcessing && alphabetizedData !== null
                    }
                    isProcessing={isProcessing}
                />
            </div>
        </section>
    );
}

