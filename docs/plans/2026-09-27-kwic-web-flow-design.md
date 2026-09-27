# KWIC web integration

## Scope and decision

Deliver the missing text-to-results web flow using the existing Java KWIC engine. A stateless endpoint is the smallest useful integration. Adding MySQL history would also require persistence models, ownership, and retention decisions; implementing Cyberminer would be a separate search-engine milestone. Those additions are outside this change.

## Architecture

`App.jsx` submits `{ "text": "..." }` to `POST /api/kwic`. `KwicController` handles HTTP and JSON errors. `KwicService` validates and normalizes the input, then calls `MasterControl.processLines`. That method reuses `StorageLineList`, `CircShiftedLineList`, and `AlphabetizedLineList`, returning a list of strings instead of console output.

All processing data is local to the request. The existing file-processing entry point and its batch workers remain available. No worker pool or database write is needed for the bounded web request.

## Contract and limits

The response contains `lines`, `inputLineCount`, and `shiftCount`. Whitespace-only lines are omitted. Sorting is case-insensitive and duplicates are retained. Limits: 10,000 UTF-16 code units, 100 non-empty lines, 50 words per line, and 1,000 words across all lines. Validation occurs before generating rotations. Invalid requests receive HTTP 400 with a `message`.

## Interface

The page includes labeled multiline input, an example, character count, limits, generate/clear controls, a results list, and a backend connection indicator. Submission disables editing until completion. Editing clears stale results. Errors preserve input and allow retry. Requests time out rather than leaving the interface busy indefinitely. The layout stacks on narrow screens.

## Environments

The explicit Spring `local` profile disables database auto-configuration. Health remains available; database health returns 503 when no datasource exists. The normal profile retains MySQL configuration through environment variables. `API_BASE_URL` selects the backend when Parcel builds the frontend, with localhost as the development fallback.

## Verification

Backend integration tests exercise the real controller, service, and processing classes, including invalid JSON/types, whitespace, limits, duplicate rotations at the maximum size, independent requests, CORS, and local startup. Frontend tests exercise submission, loading, blank input, server errors, retry, connection errors, and clearing stale results. Build both projects and smoke-test the running browser-to-backend flow at desktop and mobile widths.

## Verified integration context (2026-09-27)

These pre-existing configuration details are relevant when reviewing the integration diff:

- `backend/pom.xml` pins `spring-boot-starter-parent` to **4.1.1** and Java source/release level **21**. The local profile's `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration` exclusion and the test's `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` import match that Boot 4 version. Both context-startup tests and a running local server passed without database credentials.
- `backend/src/main/java/com/se6362/kwic/config/WebConfig.java` already configures `/api/**` with exactly two allowed origins: `https://kalani-kawaguchi.github.io` and `http://localhost:1234`. Allowed methods are `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS`; allowed headers are `*`, including `Content-Type`. No origin wildcard is configured. This existing configuration was tested with a real browser on port 1234 against the backend on port 8080, as well as the MockMvc preflight test.
- `MasterControl.processBatch` is an existing `private static` method that creates a new `CircShiftedLineList` and then a new `AlphabetizedLineList`. The new `processLines` method creates its own input and result lists. The service has no mutable request fields; the file-mode `OutputManager` and executor are not used by the HTTP flow.
- `supportsTheMaximumNumberOfRotationsIncludingDuplicates` sends 20 lines with exactly 50 words each and asserts 1,000 returned rotations. This exercises the accepted word-per-line and total-word boundaries, including the existing quicksort's repeated-key case.

Validation: `bash mvnw -B -ntp verify` passed all **12 backend tests** and packaged the executable JAR. `npm test` passed **6 frontend tests**; `npm run build` passed. Tests ran on Node 22.22.0 and Java 26 (compiling with release 21). Browser checks covered the full processing flow, validation, retry, clear, and 375px layout without horizontal overflow. Real MySQL connectivity remains outside the local-profile checks.

### Test dependencies

The new development dependencies are Vitest 5.0.2, jsdom 29.1.1, `@testing-library/react` 16.3.3, `@testing-library/user-event` 14.6.7, and `@testing-library/jest-dom` 7.0.1. They support browser-like component tests and are not imported by application code.

`obug` is a transitive development dependency declared by [Vitest's official package manifest](https://github.com/vitest-dev/vitest/blob/main/packages/vitest/package.json). Its [upstream repository](https://github.com/sxzz/obug) documents it as an ESM/TypeScript fork of `debug`. On 2026-09-27, `npm view obug@2.2.1 name version repository.url maintainers dist.integrity dist.attestations --json` returned the `sxzz/obug` repository, maintainer `sxzz`, and published SLSA provenance metadata. The registry's SHA-512 integrity matched the lockfile:

```text
sha512-XrsrhT5sybtKI6wakr2SPOlGZWWYbUXZ7a0jT8/QOeAPau+1X/bSegNe5YR75oJmEZQbKningirmGOEJCIk61Q==
```

This verifies the dependency's identity and lockfile integrity against npm metadata; it is not a full third-party source audit.
