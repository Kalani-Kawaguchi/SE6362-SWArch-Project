/*
    SE 6362.001
    Course Project: KWIC Index System
    Team Members: Jonathan Loper, Kalani Kawaguchi, Zhi Li, Mitchell Vu
    File Name: App.jsx
    Description: Root react component
 */

import { HashRouter, Routes, Route, NavLink, Link } from "react-router-dom";
import Home from "./pages/Home.jsx";
import KWIC from "./pages/KWIC.jsx";
import Search from "./pages/Search.jsx";
import { useEffect, useState } from "react";
import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import ProjDev1 from "./documents/SoftwareArchitectureProject1.pdf";

const API_BASE_URL = process.env.API_BASE_URL;

export default function App() {
  const [backendStatus, setBackendStatus] = useState(
    "Checking backend status...",
  );

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
            <NavLink to="/kwic">Index</NavLink>
            <NavLink to="/search">Search</NavLink>
          </nav>

          <span
            className={`connection ${backendStatus === "Backend connected" ? "connected" : ""
              }`}
          >
            <span className="status-dot" aria-hidden="true" />
            {backendStatus}
          </span>
        </header>

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/kwic" element={<KWIC />} />
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

    </div>
  );
}
