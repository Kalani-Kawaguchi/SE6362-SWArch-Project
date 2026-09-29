//import com.se6362.kwic.MasterControl;
import React, { useState } from "react";

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
  const [Contents, setContents] = useState("");
  const [draft, setDraft] = useState("");


  return (
    <div style={containerStyle}>
      <div>
        <label htmlFor="input" style={labelStyle}>
          User Input 
        </label>
        <textarea
          id="input"
          style = {inputStyle}
          rows = {4}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Enter your text here"
        />
      </div>

      <div>
          <button type="button" onClick={() => {
            setContents(draft)
            // call for KWIC functions here
            // variable "Contents" stores the user input
            }}
          >
            Run KWIC
          </button>

      </div>

      <div>  
        <label htmlFor="file-name" style={labelStyle}>
          Contents
        </label>
        <textarea 
          id="file-name"
          rows = {5}
          type="text"
          value={Contents}
          readOnly
          placeholder="No file selected"
          style={readOnlyInputStyle}
        />
      </div>

      <div>
        <label htmlFor="file-name" style={labelStyle}>
          Circular Shift
        </label>
        <textarea
          id="file-name"
          rows = {5}
          type="text"
          value={""}
          readOnly
          placeholder="No file selected"
          style={readOnlyInputStyle}
        />
      </div>

      <div>
        <label htmlFor="file-size" style={labelStyle}>
          Sorted Shift
        </label>
        <textarea
          id="file-size"
          rows = {5}
          type="text"
          value={""}
          readOnly
          placeholder="No file selected"
          style={readOnlyInputStyle}
        />
      </div>
    </div>
  );
}

