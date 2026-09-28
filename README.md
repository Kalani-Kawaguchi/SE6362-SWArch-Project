# SE6362 Software Architecture Project

A team project for **CS/SE 6362 - Software Architecture**

This repository contains the project for our KWIC (Key Word in Context) application. The application-specific KWIC implementation will be developed collaboratively by the team.

**Live frontend:** https://kalani-kawaguchi.github.io/SE6362-SWArch-Project/

---

## Team
- Kalani Kawaguchi
- Zhi Li
- Jonathan Loper
- Mitchell Vu

## Project Status

Project infrastructure is set up and working end to end.

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
    | JPA / Hibernate
    v
MySQL Database
    (Railway)
```

---

## Getting Started

### Prerequisites

Install the following before working locally:

- **Git**
- **Node.js / npm**
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
npm install
```

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
.\mvnw.cmd spring-boot:run
```

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
npm run deploy
```

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