/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: SearchResult.jsx
    Description: Generic react component for displaying a single result from the search engine
 */

import "./styles.css";
import "./searchResultStyles.css";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

export default function SearchResult({
    title,
    url,
    description,
}) {
    const handleClick = async () => {
        const element = {
            title: title,
            url: url,
            description: description
        };

        try {
            const response = await fetch(`${API_BASE_URL}/api/search`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(element),
                keepalive: true
            });

            if (!response.ok) {
                console.error("Failed to update webpage access frequency.");
            }
        }
        catch (error) {
            console.error("Error updating webpage access frequency:", error);
        }
    };

    return (
        <li className="search-result">
            <div className="search-result-url">
                <a
                    href={url}
                    className="search-result-favicon"
                    aria-hidden="true"
                    onClick={handleClick}
                >
                    ↗
                </a>

                <p>{url}</p>
            </div>

            <h3 className="search-result-title">
                <a
                    href={url}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={handleClick}
                >
                    {title}
                </a>
            </h3>

            <p className="search-result-description">
                {description}
            </p>
        </li>
    );
}