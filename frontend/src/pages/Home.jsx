import ProjPlan from "../documents/PreliminaryProjectPlan.pdf";
import { useEffect, useState } from "react";

const API_BASE_URL = process.env.API_BASE_URL;

export default function Home() {


    return (
        <main>
            <h1>Software Architecture Project</h1>
            <h2>Group: Kalani Kawaguchi, Zhi Li, Jonathan Loper, Mitchell Vu</h2>
            <a href={ProjPlan}>Preliminary Project Plan</a>
        </main>
    );
}
