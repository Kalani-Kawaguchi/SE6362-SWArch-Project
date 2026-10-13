import { BrowserRouter, Routes, Route, NavLink } from "react-router-dom";
import Home from "./pages/Home.jsx";
import KWIC from "./pages/KWIC.jsx";
import Search from "./pages/Search.jsx";
import { useEffect, useState } from "react";
import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import KWIC from "./pages/KWIC.jsx"

const API_BASE_URL = process.env.API_BASE_URL;

export default function App() {
  const [backendStatus, setBackendStatus] = useState(
    "Checking backend status...",
  );


  return (
    <div className="app-shell">


      <BrowserRouter>
        <header className="topbar">
          <a className="brand" href="/">
            <span className="brand-icon" aria-hidden="true">
              SE 6362
            </span>
            <span>Course Project</span>
          </a>

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
      </BrowserRouter>


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
