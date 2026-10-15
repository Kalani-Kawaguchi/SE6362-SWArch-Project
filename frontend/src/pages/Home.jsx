import ProjPlan from "../documents/PreliminaryProjectPlan.pdf";
import ProjDev1 from "../documents/SoftwareArchitectureProject1.pdf";

const API_BASE_URL = process.env.API_BASE_URL;

export default function Home() {


    return (

        <main className="home-page">
            <section className="home-hero">
                <p className="eyebrow">SE 6362 · SOFTWARE ARCHITECTURE</p>

                <h1>
                    Software Architecture
                    <span> Project</span>
                </h1>

                <p className="home-description">
                    Welcome to our course project. Explore our team and
                    project documentation below.
                </p>
            </section>

            <section className="team-section">
                <h2>Our Team</h2>

                <ul className="team-list">
                    <li>Kalani Kawaguchi (Backend Developer)</li>
                    <li>Zhi Li (UI Designer)</li>
                    <li>Jonathan Loper (System Architect)</li>
                    <li>Mitchell Vu (Frontend Developer)</li>
                </ul>
            </section>

            <section className="documentation-section">
                <p className="eyebrow">PROJECT RESOURCES</p>
                <h2>Documentation</h2>

                <a className="documentation-link" href={ProjPlan} target="_blank" rel="noreferrer">
                    <span className="document-icon" aria-hidden="true">↗</span>
                    <span className="document-text">
                        <strong>Preliminary Project Plan</strong>
                        <span>View our initial project proposal</span>
                    </span>
                    <span className="document-arrow" aria-hidden="true">→</span>
                </a>
                <a className="documentation-link" href={ProjDev1} target="_blank" rel="noreferrer">
                    <span className="document-icon" aria-hidden="true">↗</span>
                    <span className="document-text">
                        <strong>Project Phase 1 Deliverable</strong>
                        <span>View our documentation for project phase 1</span>
                    </span>
                    <span className="document-arrow" aria-hidden="true">→</span>
                </a>
            </section>
        </main>
    );
}
