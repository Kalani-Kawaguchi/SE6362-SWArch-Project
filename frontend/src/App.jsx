import { HashRouter, Routes, Route, NavLink, Link } from "react-router-dom";
import Home from "./pages/Home.jsx";
import KWIC from "./pages/KWIC.jsx";
import Search from "./pages/Search.jsx";
import { useEffect, useState } from "react";
import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";

const API_BASE_URL = process.env.API_BASE_URL;

export default function App() {
  const [backendStatus, setBackendStatus] = useState(
    "Checking backend status...",
  );


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

        <a href={ProjPlan}>
          Preliminary project plan ↗
        </a>
      </footer>

    </div>
  );
}
