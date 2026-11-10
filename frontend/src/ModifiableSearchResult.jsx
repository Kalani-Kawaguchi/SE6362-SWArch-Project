/*
    SE 6362.001
    Course Project: KWIC Index System
    File Name: ModifiableSearchResult.jsx
    Description: Search result component with webpage modification controls.
*/

import { useState } from "react";
import SearchResult from "./SearchResult.jsx";
import "./styles.css";

export default function ModifiableSearchResult({
    title,
    url,
    description,
    isDelete,
    isSponsor,
    sponsorshipLevel,
    onDeleteChange,
    onSponsorChange,
    onSponsorshipLevelChange
}) {
    const handleDeleteChange = (event) => {
        const checked = event.target.checked;

        onDeleteChange(checked);

        if (checked) {
            onSponsorChange(false);
            onSponsorshipLevelChange(0);
        }
    };

    const handleSponsorChange = (event) => {
        const checked = event.target.checked;

        onSponsorChange(checked);
        onSponsorshipLevelChange(checked ? 1 : 0);
    };

    const handleSponsorshipLevelChange = (event) => {
        const value = Number.parseInt(event.target.value, 10);

        if (Number.isNaN(value)) {
            onSponsorshipLevelChange(1);
            return;
        }

        onSponsorshipLevelChange(Math.max(1, Math.min(5, value)));
    };

    return (
        <SearchResult
            title={title}
            url={url}
            description={description}
        >
            <label className="checkbox-option">
                <input
                    type="checkbox"
                    checked={isDelete}
                    onChange={handleDeleteChange}
                />
                Delete
            </label>

            <label className="checkbox-option">
                <input
                    type="checkbox"
                    checked={isSponsor}
                    disabled={isDelete}
                    onChange={handleSponsorChange}
                />
                Sponsored
            </label>

            <label>
                Sponsorship Level
                <input
                    type="number"
                    min="0"
                    max="5"
                    step="1"
                    value={sponsorshipLevel}
                    disabled={!isSponsor || isDelete}
                    onChange={handleSponsorshipLevelChange}
                />
            </label>
        </SearchResult>
    );
}