import React, { useState } from "react";
import JsonFileForm from "./JsonFileForm";

import "./AddWebPages.css";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");

function AddWebPages({ onSuccess }) {
    const [title, setTitle] = useState("");
    const [url, setUrl] = useState("");
    const [description, setDescription] = useState("");

    const [filePages, setFilePages] = useState(null);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleManualInput() {
        setFilePages(null);
        setError("");
        setSuccess("");
    }

    function handleJsonLoaded(json) {
        setFilePages(json);
        setError("");
        setSuccess("");
    }

    function validatePage(page) {
        if (
            !page ||
            typeof page.title !== "string" ||
            typeof page.url !== "string" ||
            typeof page.description !== "string" ||
            !page.title.trim() ||
            !page.url.trim() ||
            !page.description.trim()
        ) {
            return false;
        }

        try {
            const parsedUrl = new URL(page.url);

            return ["http:", "https:"].includes(parsedUrl.protocol);
        }
        catch {
            return false;
        }
    }

    async function submitWebpages(pages) {
        const endpoint = `${API_BASE_URL}/kwic/add`;

        console.log("Submitting to:", endpoint);
        console.log("Webpages:", pages);

        const response = await fetch(`${API_BASE_URL}/api/kwic/add`,
            {
                method: "POST",
                headers:
                {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(pages)
            });
        const result = await response.text();

        console.log("Response status:", response.status);
        console.log("Response body:", result);

        if (!response.ok) {
            throw new Error(result || `Server error: ${response.status}`);
        }

        return result;
    }

    async function handleSubmit(event) {
        event.preventDefault();

        setError("");
        setSuccess("");

        let pages;

        if (filePages !== null) {
            if (
                !Array.isArray(filePages) ||
                filePages.length === 0 ||
                !filePages.every(validatePage)
            ) {
                setError("Invalid webpage JSON data.");
                return;
            }

            pages = filePages;
        }
        else {
            const page = {
                title: title.trim(),
                url: url.trim(),
                description: description.trim()
            };

            if (!validatePage(page)) {
                setError("Please provide valid webpage information.");
                return;
            }

            pages = [page];
        }

        try {
            setIsSubmitting(true);

            const result = await submitWebpages(pages);

            setSuccess(result || "Webpages submitted successfully.");
            onSuccess?.();
        }
        catch (err) {
            console.error("Webpage submission failed:", err);
            setError(err.message || "Failed to submit webpages.");
        }
        finally {
            setIsSubmitting(false);
        }
    }

    return (
        <>

            <div className="panel-heading">
                <h2 id="add-pages-title">Add Web Pages</h2>
            </div>

            <form onSubmit={handleSubmit}>

                <div className="page-input-fields">

                    <div className="page-input-row">

                        <input
                            type="text"
                            placeholder="Title"
                            value={title}
                            disabled={filePages !== null || isSubmitting}
                            onChange={(event) => {
                                handleManualInput();
                                setTitle(event.target.value);
                            }}
                        />

                        <input
                            type="url"
                            placeholder="URL"
                            value={url}
                            disabled={filePages !== null || isSubmitting}
                            onChange={(event) => {
                                handleManualInput();
                                setUrl(event.target.value);
                            }}
                        />

                    </div>

                    <textarea
                        placeholder="Description"
                        value={description}
                        disabled={filePages !== null || isSubmitting}
                        onChange={(event) => {
                            handleManualInput();
                            setDescription(event.target.value);
                        }}
                    />

                </div>

                <div className="upload-divider">
                    <span>OR</span>
                </div>

                <JsonFileForm
                    onJsonLoaded={handleJsonLoaded}
                />

                {error && (
                    <p className="error" role="alert">
                        {error}
                    </p>
                )}

                {success && (
                    <p className="success" role="status">
                        {success}
                    </p>
                )}

                <div className="add-pages-actions">
                    <button
                        type="submit"
                        className="primary-button"
                        disabled={isSubmitting}
                    >
                        <span>
                            {isSubmitting ? "Submitting..." : "Submit"}
                        </span>
                        <span>→</span>
                    </button>
                </div>

            </form>

        </>
    );
}

export default AddWebPages;