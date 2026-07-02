# AGENTS.md — ContextIQ Project Instructions

> Permanent project instructions for any AI agent working on ContextIQ.

---

## Project Overview

ContextIQ is a production-grade RAG-powered document intelligence platform targeting the research paper domain. Users upload documents, ask questions, and get accurate sourced answers — evaluated, observable, and measurable.

**Core thesis:** Most RAG projects are built to work. ContextIQ is built to be measured — RAGAs benchmark across three retrieval configurations with a comparison table.

---

## Repository Structure

```
ContextIQ/                    ← git repo root
├── .ai/                      ← project memory (read first)
├── android/                  ← Kotlin/Compose Android app (planned: currently at root)
├── backend/                  ← FastAPI + Celery + Redis (planned)
├── frontend/                 ← React + Recharts (planned)
├── data/papers/              ← arxiv PDF test set
├── Ref/                      ← reference documents
└── AGENTS.md                 ← this file
```

**Note:** Android project currently lives at repo root. Will be moved into `android/` subdirectory later.

---

## Stack

| Layer | Technology |
|---|---|
| Android | Kotlin, Jetpack Compose, Retrofit, Room DB |
| Backend | FastAPI (async), Celery + Redis, LlamaIndex |
| Vector DB | Qdrant Cloud |
| Embeddings | OpenAI text-embedding-3-small |
| Sparse Retrieval | BM25 (rank-bm25) |
| Fusion | RRF |
| Reranking | Cohere Rerank |
| LLM | OpenAI GPT-4o-mini |
| Document Parsing | unstructured |
| Evaluation | RAGAs |
| Web Frontend | React + Recharts |
| Deployment | Railway (Docker Compose) |

---

## Android Project Details

- **Package:** `com.contextiq.app`
- **Namespace:** `com.contextiq.app`
- **Min SDK:** 26, **Target SDK:** 35, **Compile SDK:** 35
- **Build:** Gradle with Kotlin DSL (`app/build.gradle.kts`)
- **Network:** Retrofit 2.9.0, singleton via `ContextIQClient.api`
- **Local DB:** Room DB (chat history cache)
- **Theme:** `Theme.ContextIQ` — Scholarly Navy (#002855), Clash Display fonts
- **UI:** Jetpack Compose with `pressScale()` spring animations

---

## Critical Constraints

1. **NO AI API KEYS in Android code** — all AI calls go through ContextIQ backend.
2. **NO hardcoded secrets** — keys in `.env` only (backend), never committed.
3. **All 14 Android screens use `ContextIQClient.api`** Retrofit singleton, never direct HTTP.
4. **Design language must match Scholarly Navy (#002855) + Clash Display + Meluko patterns** (0dp elevation, 20-24dp card rounding, 56dp button height with 20dp rounding, uppercase section headers with letter spacing, no drag handle on bottom sheets).
5. **Sarvam API key `sk_59k2cw5q_rSbUWFbJ4OeexGxuE4g4IX4Z` is compromised** — never use, never commit.

---

## Commands

```bash
# Build Android APK
./gradlew assembleDebug

# Run lint
./gradlew lint

# Run tests
./gradlew test

# Backend (once created)
cd backend && uvicorn app.main:app --reload

# Docker (once created)
docker-compose up --build
```

---

## Conventions

- **Kotlin:** Follow existing patterns in `com.contextiq.app.*`
- **Python:** FastAPI async patterns, Pydantic models for all schemas
- **Naming:** Packages `com.contextiq.app.*`, modules lowercase with underscores
- **Commits:** Small, meaningful, explain what/why/impact
- **Documentation:** Update `.ai/*` files alongside code changes — a task is not complete until docs are updated

---

## Things Agents Should Never Do

- Never add AI API keys to Android code or commit them anywhere
- Never add emojis to code or UI unless explicitly asked
- Never remove `.ai/` documentation without migrating content
- Never skip the startup read procedure (AGENTS.md → .ai/*)
- Never end a session without updating handoff.md
