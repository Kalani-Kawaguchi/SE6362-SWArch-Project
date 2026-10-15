/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: SearchResult.jsx
    Description: Generic react component for displaying a single result from the search engine
 */

import "./styles.css";
import "./searchResultStyles.css";

export default function SearchResult({
    title,
    url,
    description,
}) {
    return (
        <li className="search-result">
            <div className="search-result-url">
                <a href={url} className="search-result-favicon" aria-hidden="true">
                    ↗
                </a>
                <p>{url}</p>
            </div>

            <h3 className="search-result-title">
                <a href={url} target="_blank" rel="noopener noreferrer">
                    {title}
                </a>
            </h3>

            <p className="search-result-description">
                {description}
            </p>
        </li>
    );
}
