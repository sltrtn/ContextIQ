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
| Backend | FastAPI (async), LlamaIndex, Pydantic |
| Vector DB | Qdrant (in-memory, `:memory:`) |
| Embeddings | fastembed (BAAI/bge-small-en-v1.5, 384d, local) |
| Sparse Retrieval | BM25 (rank-bm25), global singleton |
| Fusion | RRF (K=60) |
| Reranking | Cohere Rerank (rerank-english-v3.0) |
| LLM | Groq (Llama 3.3 70B Versatile, free) |
| Document Parsing | pypdf (PDF), python-docx (DOCX) |
| Evaluation | Custom LLM-as-judge (faithfulness, relevancy, precision, recall) |
| Web Frontend | React + Recharts (planned) |
| Deployment | Railway (Docker Compose, planned) |

---

## Android Project Details

- **Package:** `com.contextiq.app`
- **Namespace:** `com.contextiq.app`
- **Min SDK:** 26, **Target SDK:** 35, **Compile SDK:** 35
- **Build:** Gradle with Kotlin DSL (`app/build.gradle.kts`)
- **Network:** Retrofit 2.9.0, singleton via `ContextIQClient.api`
- **Local DB:** Room DB (chat history cache)
- **Theme:** `Theme.ContextIQ` — Scholarly Navy (#002855), Outfit fonts (see `.ai/design-system.md`)
- **UI:** Jetpack Compose with `pressScale()` spring animations

---

## Critical Constraints

1. **NO AI API KEYS in Android code** — all AI calls go through ContextIQ backend.
2. **NO hardcoded secrets** — keys in `.env` only (backend), never committed.
3. **All 14 Android screens use `ContextIQClient.api`** Retrofit singleton, never direct HTTP.
4. **Design language must follow `.ai/design-system.md`** — Outfit font, Scholarly Navy (#002855) accent, shared neutral tokens, 0dp elevation, 12-16dp card rounding, 48px button height with 20dp rounding, uppercase section headers with letter spacing, no drag handle on bottom sheets.
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

# Run backend server
cd backend && source venv/bin/activate && uvicorn app.main:app --reload --port 8000

# Test health
curl http://localhost:8000/api/v1/health

# Upload document
curl -s -X POST http://localhost:8000/api/v1/documents/upload \
  -F "file=@data/papers/2305.18290_DPO.pdf"

# Query (full pipeline)
curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is QLoRA?", "top_k": 5, "config": "hybrid_rerank"}'

# Query with expansion
curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is QLoRA?", "expand": true}'

# Run evaluation
curl -s -X POST http://localhost:8000/api/v1/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"config": "vector_only", "max_questions": 5}'

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
