//import com.se6362.kwic.MasterControl;
import React, { useState } from "react";

const containerStyle = {
  maxWidth: 400,
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
  const [fileName, setFileName] = useState("");
  const [fileSize, setFileSize] = useState("");

  return (
    <div style={containerStyle}>
      <div>
        <label htmlFor="file-upload" style={labelStyle}>
          Upload a .json file
        </label>
        <input
          id="file-upload"
          type="file"
          accept=".json,application/json" 
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (!file) {
              setFileName("");
              setFileSize("");
              return;
            }
            setFileName(file.name);
            setFileSize(`${(file.size / 1024).toFixed(1)} KB`);
          }}
        />
      </div>

      <div>  
        <label htmlFor="file-name" style={labelStyle}>
          File Contents
        </label>
        <textarea
          id="file-name"
          type="text"
          value={""}
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

