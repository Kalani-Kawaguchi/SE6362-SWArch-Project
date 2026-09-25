import ProjPlan from "./documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";
import KWIC from "./KWIC.jsx"

const API_BASE_URL = process.env.API_BASE_URL;

export default function App() {
  const [backendStatus, setBackendStatus] = useState(
    "Checking backend status...",
  );

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/health`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Backend request failed from the frontend");
        }

        return response.text();
      })
      .then((data) => {
        setBackendStatus(data);
      })
      .catch((error) => {
        console.error(error);
        setBackendStatus("Unable to connect to backend");
      });
  }, []);

  return (
    <main>
      <h1>Software Architecture Project</h1>
      <h2>Group: Kalani Kawaguchi, Zhi Li, Jonathan Loper, Mitchell Vu</h2>
      <a href={ProjPlan}>Preliminary Project Plan</a>

      <h3>Backend Status</h3>
      <p>{backendStatus}</p>

      <KWIC/>
      
    </main>
  );
}
