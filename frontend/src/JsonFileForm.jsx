
import React, { useState } from "react";
import "./AddWebPages.css";

function JsonFileForm({ onJsonLoaded }) {
    const [file, setFile] = useState(null);
    const [error, setError] = useState("");

    async function handleFileChange(event) {
        const selectedFile = event.target.files[0];

        setError("");
        setFile(null);
        onJsonLoaded(null);

        if (!selectedFile) {
            return;
        }

        try {
            if (!selectedFile.name.toLowerCase().endsWith(".json")) {
                throw new Error("Please select a JSON file.");
            }

            const text = await selectedFile.text();
            const json = JSON.parse(text);

            if (!Array.isArray(json)) {
                throw new Error("JSON file must contain an array.");
            }

            setFile(selectedFile);
            onJsonLoaded(json);
        }
        catch (err) {
            setError(err.message);
            event.target.value = "";
        }
    }

    return (
        <div className="json-file-form">
            <div className="json-upload">
                <input
                    id="json-file"
                    type="file"
                    accept=".json,application/json"
                    onChange={handleFileChange}
                />

                <label
                    htmlFor="json-file"
                    className="file-select-button"
                >
                    Choose a JSON file
                </label>
            </div>

            {file && (
                <p className="file-info">
                    Selected: {file.name}
                </p>
            )}

            {error && (
                <p className="error">{error}</p>
            )}
        </div>
    );
}

export default JsonFileForm;
