import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";

const result = {
  inputLineCount: 1,
  shiftCount: 3,
  lines: ["architecture project software", "project software architecture", "software architecture project"],
};

function mockApi(processRequest) {
  const fetchMock = vi.fn((url, options) => {
    if (url.endsWith("/api/health")) {
      return Promise.resolve({ ok: true, text: async () => "KWIC backend is running!" });
    }
    return processRequest(url, options);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("KWIC web flow", () => {
  beforeEach(() => {
    mockApi(async () => ({ ok: true, json: async () => result }));
  });

  it("submits text to the API and displays the sorted rotations", async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByRole("textbox", { name: /input text/i }), "software architecture project");
    await user.click(screen.getByRole("button", { name: /generate index/i }));
    expect(await screen.findByText("architecture project software")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem").map((item) => item.textContent)).toEqual(result.lines);
    expect(fetch).toHaveBeenCalledWith("http://localhost:8080/api/kwic", expect.objectContaining({
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text: "software architecture project" }),
    }));
  });

  it("does not submit whitespace-only input", async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByRole("textbox", { name: /input text/i }), "   ");
    expect(screen.getByRole("button", { name: /generate index/i })).toBeDisabled();
    expect(fetch.mock.calls.filter(([url]) => url.endsWith("/api/kwic"))).toHaveLength(0);
  });

  it("prevents repeat submissions while processing", async () => {
    let finish;
    mockApi(() => new Promise((resolve) => { finish = resolve; }));
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByRole("textbox", { name: /input text/i }), "software architecture project");
    await user.click(screen.getByRole("button", { name: /generate index/i }));
    expect(screen.getByRole("button", { name: /generating/i })).toBeDisabled();
    expect(screen.getByRole("textbox", { name: /input text/i })).toBeDisabled();
    finish({ ok: true, json: async () => result });
    await screen.findByText("architecture project software");
    expect(screen.getByRole("button", { name: /generate index/i })).toBeEnabled();
  });

  it("shows server validation errors and lets the user retry", async () => {
    mockApi(async () => ({ ok: false, status: 400, json: async () => ({ message: "Use at most 50 words per line." }) }));
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByRole("textbox", { name: /input text/i }), "a b");
    await user.click(screen.getByRole("button", { name: /generate index/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Use at most 50 words per line.");
    mockApi(async () => ({ ok: true, json: async () => result }));
    await user.click(screen.getByRole("button", { name: /generate index/i }));
    await screen.findByText("architecture project software");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("handles a network failure without losing the input", async () => {
    mockApi(async () => { throw new TypeError("Failed to fetch"); });
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByRole("textbox", { name: /input text/i }), "a b");
    await user.click(screen.getByRole("button", { name: /generate index/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/unable to connect/i);
    expect(screen.getByRole("textbox", { name: /input text/i })).toHaveValue("a b");
  });

  it("clears old results when input changes", async () => {
    const user = userEvent.setup();
    render(<App />);
    await user.type(screen.getByRole("textbox", { name: /input text/i }), "software architecture project");
    await user.click(screen.getByRole("button", { name: /generate index/i }));
    await screen.findByText("architecture project software");
    await user.type(screen.getByRole("textbox", { name: /input text/i }), " extra");
    await waitFor(() => expect(screen.queryByText("architecture project software")).not.toBeInTheDocument());
  });
});
