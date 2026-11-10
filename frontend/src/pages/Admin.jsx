
import { useState } from "react";
import SearchInterface from "../shared/SearchInterface.jsx";
import ModifiableSearchResult from "../ModifiableSearchResult.jsx";
import AddWebPages from "../AddWebPages.jsx";
import "../styles.css";

const API_BASE_URL = (
    process.env.API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

function createInitialSettings(results) {
    return Object.fromEntries(
        results.map((result) => {
            const sponsorshipLevel = result.sponsorship_level ?? 0;

            return [
                result.url,
                {
                    isDelete: false,
                    isSponsor: sponsorshipLevel > 0,
                    sponsorshipLevel
                }
            ];
        })
    );
}

function haveSettingsChanged(current, original) {
    const urls = Object.keys(original);

    return urls.some((url) => {
        const currentSetting = current[url];
        const originalSetting = original[url];

        return (
            !currentSetting ||
            currentSetting.isDelete !== originalSetting.isDelete ||
            currentSetting.isSponsor !== originalSetting.isSponsor ||
            currentSetting.sponsorshipLevel !== originalSetting.sponsorshipLevel
        );
    });
}

export default function Admin() {
    const [originalSettings, setOriginalSettings] = useState({});
    const [settings, setSettings] = useState({});
    const [searchResults, setSearchResults] = useState([]);
    const [refreshTrigger, setRefreshTrigger] = useState(0);

    const hasChanges = haveSettingsChanged(settings, originalSettings);

    const handleResultsLoaded = (results) => {
        const initialSettings = createInitialSettings(results);

        setSearchResults(results);
        setOriginalSettings(initialSettings);
        setSettings(initialSettings);
    };

    const handleSearchStarted = () => {
        setSearchResults([]);
        setOriginalSettings({});
        setSettings({});
    };

    const handleCancel = () => {
        setSettings({ ...originalSettings });
    };

    const updateSetting = (url, setting, value) => {
        setSettings((previousSettings) => ({
            ...previousSettings,
            [url]: {
                ...previousSettings[url],
                [setting]: value
            }
        }));
    };

    const handleSave = async () => {
        const deletionPayload = searchResults
            .filter((result) => settings[result.url]?.isDelete)
            .map((result) => ({
                title: result.title,
                url: result.url,
                description: result.description
            }));

        const sponsorshipPayload = searchResults
            .filter((result) => {
                const current = settings[result.url];
                const original = originalSettings[result.url];

                return (
                    current.sponsorshipLevel !== original.sponsorshipLevel
                );
            })
            .map((result) => ({
                title: result.title,
                url: result.url,
                description: result.description,
                sponsorship_level: settings[result.url].sponsorshipLevel
            }));

        try {
            if (deletionPayload.length > 0) {
                const response = await fetch(
                    `${API_BASE_URL}/api/kwic/delete`,
                    {
                        method: "DELETE",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify(deletionPayload)
                    }
                );

                if (!response.ok) {
                    throw new Error("Failed to delete webpages.");
                }
            }

            if (sponsorshipPayload.length > 0) {
                const response = await fetch(
                    `${API_BASE_URL}/api/kwic/sponsor`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify(sponsorshipPayload)
                    }
                );

                if (!response.ok) {
                    throw new Error("Failed to update webpage sponsorships.");
                }
            }

            const updatedSettings = { ...settings };

            setOriginalSettings(updatedSettings);
            setRefreshTrigger((previous) => previous + 1);

            alert("Changes saved successfully.");
        }
        catch (error) {
            console.error("Error saving admin changes:", error);
            alert(error.message);
        }
    };

    const getResultProps = (result) => {
        const resultSettings = settings[result.url] ?? {
            isDelete: false,
            isSponsor: false,
            sponsorshipLevel: 0
        };

        return {
            ...resultSettings,

            onDeleteChange: (value) =>
                updateSetting(result.url, "isDelete", value),

            onSponsorChange: (value) =>
                updateSetting(result.url, "isSponsor", value),

            onSponsorshipLevelChange: (value) =>
                updateSetting(result.url, "sponsorshipLevel", value)
        };
    };

    return (
        <div className="admin-page">
            <SearchInterface
                ResultComponent={ModifiableSearchResult}
                getResultProps={getResultProps}
                onResultsLoaded={handleResultsLoaded}
                onSearchStarted={handleSearchStarted}
                refreshTrigger={refreshTrigger}
            />

            <div className={`admin-action-bar ${hasChanges ? "is-visible" : ""}`}>
                <button
                    type="button"
                    className="admin-cancel-button"
                    onClick={handleCancel}
                >
                    Cancel Changes
                </button>

                <button
                    type="button"
                    className="admin-save-button"
                    onClick={handleSave}
                >
                    Save Changes
                </button>
            </div>

        </div>
    );
}
