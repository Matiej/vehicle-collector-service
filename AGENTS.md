# AGENTS.md — vehicle-collector-service

## What this project is

Backend of a vehicle photo collection system: **Kotlin + Spring Boot 3.5 (WebFlux), Java 21, MongoDB**.
Ecosystem:

- **PWA** (photo capture) → `/api/app/**`, Bearer JWT from Keycloak.
- **Web panel** (React, repo `G:\REACT\vehicle-collector-fe\`) → `/api/web/**`, cookie session / BFF (epic E004).
- **vc-rag-service** (FastAPI + pgvector) — the recognition engine. **Only this service** calls it.

**CQRS — hard rule:** Mongo is the read model that the panel and the PWA browse. RAG is not on the
browsing path, and only this service serves files and thumbnails. Frontends never talk to RAG.

## Read at session start

1. `info/vc-kotlin-architecture.md` — architecture of this service, decisions D-1…D-12.
2. The current session plan / cheat sheet in `info/` (e.g. `info/backend-catchup-2026-10-09.md`).
3. The current task card in `info/jira/` and its epic in `info/epic/`.
4. For `/api/web/**` endpoints: contracts in the FE repo `/mnt/g/REACT/vehicle-collector-fe/info/contracts/`
   and the board `/mnt/g/REACT/vehicle-collector-fe/info/backend-frontend_agreements.md`.
5. For RAG integration: `info/backend-dev-context.md` (the RAG service's living document, not this service's).

`info/` is in `.gitignore` on purpose — documentation stays local. A clean `git status` does not mean
changes in `info/` were not saved.

## How we work

- **Claude writes the code; Maciek reviews by reading and tests live.**
- **Maciek commits, merges and creates branches himself.** Claude does not commit unless explicitly asked.
- **Small steps:** one task at a time, with a card in `info/jira/`. When a task is done, add a
  "Jak zrobione" section (implementation decisions) to the card and update its status.
- **Language:** code, identifiers and commit messages in English. Conversation and documents in `info/`
  in Polish.
- **No comments in code** (neither inline nor KDoc). Names carry the intent; the reasoning behind
  decisions goes to `info/`.

## Tasks and branches

- Cards: `info/jira/EMAT-0xx_<epic>_<description>.md`, epics: `info/epic/E0xx_*.md`.
- Branch: `dev/EMAT-0xx_<short_description>`, commit: `EMAT-0xx | <description in English>`.

## Contracts

- **Contract-first for `/api/web/**`:** the contracts in the FE repo are the source of truth. A shape
  change means an entry on the FE board, never a silent change in code. A `FROZEN` contract changes
  only with the agreement of both sides.
- After every `/api/app/**` contract change — an entry in `info/pwa_developer_info.md` (§1a status, §5a changelog).
- After every `/api/web/**` change — an entry in `info/web_developer_info.md` and on the board.
- Someone else's or an unknown resource → `404`, never `403` (D-6b).

## Code structure

- `api/` — controllers and DTOs: `api/web` (panel), `api/admin`, `App*` controllers (PWA).
- `assets/`, `session/`, `vehuser/` — domain logic: "what and when".
- `infrastructure/<area>` — **every adapter to an external system** (Keycloak, storage, geocoding,
  RAG in the future). The interface (port) lives next to the adapter. The domain never talks HTTP itself.
- Reactor time-based operators (`timeout`, `delaySubscription`, `Retry.backoff`) get an **explicit
  scheduler**. The default `parallel()` can starve them.

## Configuration — local and server are separate

The real `application.yml` files with secrets live **outside the repo**, separately on the local
machine and on the server. Working copies are in `info/configs/`:

- `info/configs/local/…` — local machine, Keycloak `http://192.168.50.34:8085`, realm `vehicle-local`.
- `info/configs/server/…` — server (docker), realm `vehicle`, public address `auth.vehicle-collector.com`.

**Claude writes every configuration change into both copies; Maciek copies it to the machines by hand.**
Call out every such change explicitly in the summary. Secrets never go into `application*.yml` in the repo.

## Build and tests

- `mvn test` — unit tests (JUnit 5, Kotlin, `src/test/kotlin`). **No Docker required.**
- `mvn verify` — adds integration tests (Groovy + Spock, Testcontainers, `src/integration/groovy`,
  `*IT` classes, full packages `com.emat.vehicle_collector_service…`). Requires Docker.
- Base class for `/api/**` tests: `src/integration/groovy/…/support/PublicApiSpec.groovy`.
- `VehicleCollectorServiceApplicationTests.contextLoads` is a known failure — ignore it.
- Testcontainers saying "Could not find a valid Docker environment" while `docker ps` works usually
  means a rejected Docker API version, not a missing Docker — see `src/integration/resources/docker-java.properties`.
- The WSL distro used by Claude has no Docker, so `mvn verify` has to be run by Maciek.
- Locally, after Mongo model changes, old documents may fail to map — drop the collections.

## Status as of 2026-10-11

- E001 (PWA API) and E002 (list + detail `/api/web/photos`) are merged.
- Order: **EMAT-025** (B-1/B-2, detail fixes requested by FE) → **E004 auth** EMAT-021 → 022 → 023 → 024
  → F4 sessions + upload → F5 albums → catalogue → F3 actions → Semantic Search.
- Auth goes before new endpoints so they do not have to be reworked for the session later.
