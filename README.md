# SE6362 Software Architecture Project

A team project for **CS/SE 6362 - Software Architecture**

This repository contains our KWIC (Key Word in Context) application. Enter text in the web interface to generate circular word shifts and sort them alphabetically using the Java KWIC engine.

**Live frontend:** https://kalani-kawaguchi.github.io/SE6362-SWArch-Project/

---

## Team
- Kalani Kawaguchi
- Zhi Li
- Jonathan Loper
- Mitchell Vu

## Project Status

The KWIC web flow connects the React interface to the existing Java processing engine through `POST /api/kwic`. It accepts multiple lines, returns sorted rotations, and reports input and connection errors. Each request is processed independently in memory; results are not saved to MySQL. Saved history and Cyberminer remain future work.

Current setup:
- React frontend
- Parcel dev/build tool
- GitHub Pages frontend hosting
- Java + Spring Boot REST backend
- Railway backend hosting
- Railway MySQL database
- CORS config for GitHub Pages
- ENV-based frontend API config
- Health endpoints for verifying backend and database connectivity
- KWIC text input, example, results, and validation
- Database-free local development profile
- Backend integration tests and frontend interaction tests

---

## Architecture

```text
GitHub Pages
    |
    | HTTPS / REST
    v
React Frontend
    |
    | API requests
    v
Spring Boot Backend
    |   (Railway)
    | JPA / Hibernate (infrastructure; not used by KWIC processing)
    v
MySQL Database
    (Railway)
```

KWIC request flow:

```text
App.jsx → POST /api/kwic → KwicController → KwicService
    → MasterControl.processLines → StorageLineList
    → CircShiftedLineList → AlphabetizedLineList → JSON results
```

The existing file-based `MasterControl.runKwicSystem` entry point remains available. It reads JSON lines with a `text` field and prints output; the web endpoint accepts a JSON object with a multiline string.

---

## Getting Started

### Prerequisites

Install the following before working locally:

- **Git**
- **Node.js 22.12+ / npm**
- **Java 21**

Maven does not need to be installed

Clone the repository:

```bash
git clone https://github.com/Kalani-Kawaguchi/SE6362-SWArch-Project.git
cd SE6362-SWArch-Project
```

---

## Frontend Setup

Move into the frontend directory:

```bash
cd frontend
```

Install dependencies:

```bash
npm ci
npm start
```

Open **http://localhost:1234**. The frontend uses `http://localhost:8080` by default. To override it, copy `frontend/.env.example` to `frontend/.env` and set `API_BASE_URL`. Restart Parcel after changing the environment file. This value is embedded during the build.

### Windows PowerShell Note

If PowerShell blocks `npm.ps1` because script execution is disabled, use the `.cmd` executable instead:

```powershell
npm.cmd install
npm.cmd start
```

---

## Backend Setup

Move into the backend directory:

```bash
cd backend
```

### Windows

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### macOS / Linux

```bash
bash mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Keep the backend running in a separate terminal. The `local` profile disables database auto-configuration so KWIC works without MySQL or Railway credentials. `/api/health` works normally; `/api/db-health` returns **503** because no database is configured.

For a database-backed environment, omit the `local` profile and supply `SPRING_DATASOURCE_URL` (a JDBC MySQL URL), `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` through the environment. Do not commit credentials.

## KWIC API

Send `Content-Type: application/json` to `POST /api/kwic`:

```json
{"text":"software architecture project"}
```

Successful response:

```json
{
  "lines": [
    "architecture project software",
    "project software architecture",
    "software architecture project"
  ],
  "inputLineCount": 1,
  "shiftCount": 3
}
```

Blank lines are ignored, surrounding whitespace is stripped, and words are separated by whitespace. Sorting ignores capitalization; punctuation, spelling, and duplicate rotations are preserved. Limits are 10,000 characters (UTF-16 code units), 100 non-empty lines, 50 words per line, and 1,000 words total. These bounds limit expansion and the work performed by the existing sorting engine.

Invalid JSON, missing/non-string/blank text, or input exceeding a limit returns **400** with a readable `message`, for example:

```json
{"message":"Use at most 50 words per line."}
```

## Validation

From `backend/`, build the executable JAR and run all backend tests:

```bash
bash mvnw verify
```

On Windows, use `.\mvnw.cmd verify`. Tests activate the `local` profile automatically and do not require a live database.

From `frontend/`:

```bash
npm test
npm run build
```

The tests cover processing, normalization, input validation, request isolation, CORS, local health, and UI submission/loading/error/retry behavior. The local test profile does not validate a real MySQL connection.

---

## Health Endpoints

The skeleton includes two simple endpoints for verifying infrastructure:

### Backend Health

```http
GET /api/health
```

Expected response:

```text
KWIC backend is running!
```

### Database Health

```http
GET /api/db-health
```

This endpoint performs a simple database query to verify that Spring Boot can communicate with MySQL.

Expected response:

```text
Database connection is healthy!
```

These endpoints are infrastructure checks and are not part of the final KWIC feature set.

---

## Deployment

### Frontend — GitHub Pages

The frontend is built with Parcel and published to the `gh-pages` branch.

From `frontend/`:

```bash
API_BASE_URL=https://backend-production-ffdf.up.railway.app npm run build
npx gh-pages -d dist
```

Alternatively, set the deployed backend URL in `frontend/.env` before using the existing Windows deployment script:

```powershell
npm run deploy
```

`postdeploy` uses Windows `rmdir` syntax, so macOS/Linux users should use the build and `gh-pages` commands above. Deploy the backend containing `/api/kwic` before publishing the updated frontend. A production frontend must be built with the production API URL, not localhost.

On Windows PowerShell, if needed:

```powershell
npm.cmd run deploy
```

### Backend — Railway

The Railway `Backend` service is connected to this GitHub repository with:

```text
Root Directory: /backend
```

Changes pushed to the configured branch trigger a Railway backend deployment.
