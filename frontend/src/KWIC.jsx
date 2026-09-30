//import com.se6362.kwic.MasterControl;
import React, { useState } from "react";
const API_BASE_URL = process.env.API_BASE_URL || "http://localhost:8080";

const containerStyle = {
  maxWidth: 500,
  margin: "0 auto",
  padding: 24,
  background: "#ffffff",
  borderRadius: 8,
  border: "1px solid #e0e0e0",
  display: "flex",
  flexDirection: "column",
  gap: 16,
  fontFamily: "sans-serif",
}

const labelStyle = {
  display: "block",
  fontSize: 14,
  fontWeight: 500,
  color: "#374151",
  marginBottom: 4,
}

const inputStyle = {
  display: "block",
  width: "100%",
  boxSizing: "border-box",
  fontSize: 14,
  color: "#374151",
  border: "1px solid #d1d5db",
  borderRadius: 6,
  padding: "8px 12px",
}

const readOnlyInputStyle = {
  ...inputStyle,
  background: "#f9fafb",
}

export default function KWIC() {
  const [draft, setDraft] = useState("");
  const [sortedShifts, setSortedShifts] = useState("");
  const [circularShifts, setCircularShifts] = useState("");
  const [alphabetizer, setAlphabetizer] = useState("");
  const [isRunning, setIsRunning] = useState(false);


  return (
    <div style={containerStyle}>
      <div>
        <label htmlFor="input" style={labelStyle}>
          User Input
        </label>
        <textarea
          id="input"
          style={inputStyle}
          rows={4}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Enter your text here"
        />
      </div>

      <div>
        <button type="button" onClick={async () => {
          // call for KWIC functions here
          // variable "Contents" stores the user input
          const inputData = { text: draft }
          const json = JSON.stringify(inputData, null, 2);

          console.log("KWIC input JSON:", json);
          setSortedShifts("");
          setCircularShifts("");
          setAlphabetizer("");
          setIsRunning(true);

          try {
            const response = await fetch(`${API_BASE_URL}/api/kwic`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: json,
            });

            if (!response.ok) {
              throw new Error(`Backend returned HTTP ${response.status}`);
            }

            const result = await response.json();
            console.log("KWIC backend response:", JSON.stringify(result));

            let shifts = "";
            let alpha = "";
            const output = result.output;

            result.circularShift.forEach(lines => {
              shifts += lines + '\n';
            });
            setCircularShifts(shifts);

            result.alphabetizer.forEach(lines => {
              alpha += lines + '\n';
            });
            setAlphabetizer(alpha);

            setSortedShifts(output);
          } catch (error) {
            console.error("Unable to send KWIC input:", error);
          } finally {
            setIsRunning(false);
          }
        }}
        >
          {isRunning ? "Running..." : "Run KWIC"}
        </button>

      </div>

      <div>
        <label htmlFor="file-name" style={labelStyle}>
          Circular Shift
        </label>
        <textarea
          id="file-name"
          rows={5}
          type="text"
          value={circularShifts}
          readOnly
          placeholder="Circular shifted lines will display here."
          style={readOnlyInputStyle}
        />
      </div>

      <div>
        <label htmlFor="file-name" style={labelStyle}>
          Alphabetizer
        </label>
        <textarea
          id="file-name"
          rows={5}
          type="text"
          value={alphabetizer}
          readOnly
          placeholder="Alphabetized lines will display here."
          style={readOnlyInputStyle}
        />
      </div>

      <div>
        <label htmlFor="file-size" style={labelStyle}>
          Final Output
        </label>
        <textarea
          id="file-size"
          rows={20}
          type="text"
          value={sortedShifts}
          readOnly
          placeholder="Final index will display here."
          style={readOnlyInputStyle}
        />
      </div>
    </div>
  );
}

