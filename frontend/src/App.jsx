/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: App.jsx
    Description: Root react component
 */

import { HashRouter, Routes, Route, NavLink, Link } from "react-router-dom";
import Home from "./pages/Home.jsx";
import Admin from "./pages/Admin.jsx";
import Search from "./pages/Search.jsx";
import AddWebPages from "./AddWebPages.jsx";
import { useEffect, useState } from "react";
import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import ProjDev1 from "./documents/SoftwareArchitectureProject1.pdf";

const API_BASE_URL = process.env.API_BASE_URL;

export default function App() {
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [backendStatus, setBackendStatus] = useState(
    "Checking backend status...",
  );

  const handlePagesAdded = () => {
    // Refresh the current search after adding webpages.
    setIsAddModalOpen(false);
  };

  // Check whether the backend is reachable.
  useEffect(() => {
    let cancelled = false;

    fetch(API_BASE_URL, { mode: "no-cors" })
      .then(() => {
        if (!cancelled) {
          setBackendStatus("Backend connected");
        }
      })
      .catch(() => {
        if (!cancelled) {
          setBackendStatus("Backend unavailable");
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);


  return (
    <div className="app-shell">


      <HashRouter>
        <header className="topbar">
          <Link className="brand" to="/">
            <span className="brand-icon" aria-hidden="true">
              SE 6362
            </span>
            <span>Course Project</span>
          </Link>

          <nav>
            <NavLink to="/">Home</NavLink>
            <NavLink to="/search">User View</NavLink>
            <NavLink to="/kwic">Admin View</NavLink>

          </nav>

          <button
            type="button"
            className="admin-add-button"
            onClick={() => setIsAddModalOpen(true)}
          >
            + Add Webpages
          </button>
        </header>

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/kwic" element={<Admin />} />
          <Route path="/search" element={<Search />} />
        </Routes>
      </HashRouter>


      <footer>
        <div>
          <strong>
            SE 6362 · Software Architecture
          </strong>

          <p>
            Kalani Kawaguchi · Zhi Li · Jonathan Loper · Mitchell Vu
          </p>
        </div>
        <div>
          <a href={ProjPlan}>
            Preliminary project plan ↗
          </a>
          <a href={ProjDev1}>
            Project phase 1 deliverable ↗
          </a>
        </div>

      </footer>


      {isAddModalOpen && (
        <div
          className="add-pages-overlay"
          onClick={() => setIsAddModalOpen(false)}
        >
          <div
            className="add-pages-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="add-pages-title"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              className="add-pages-close"
              aria-label="Close"
              onClick={() => setIsAddModalOpen(false)}
            >
              &times;
            </button>

            <AddWebPages
              onSuccess={handlePagesAdded}
            />
          </div>
        </div>
      )}
    </div>


  );
}
