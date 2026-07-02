# ContextIQ — Project Overview

> Living documentation for the ContextIQ project.

---

## What It Is

A production-grade RAG-powered document intelligence platform targeting the research paper domain. Users upload documents, ask questions, and get accurate sourced answers — with the system being evaluated, observable, and measurable, not just functional.

**Core thesis:** Most RAG projects are built to work. ContextIQ is built to be measured. RAGAs benchmarks across three retrieval configurations produce a comparison table with real numbers. Live observability metrics per query.

**Portfolio role:** AI credibility card — covers ML, Backend, and Android roles from a single project.

**Origin story:** Built Scholium (Kotlin Android research assistant, 12 screens, Gemini vision + Sarvam AI). Identified its core failure — no semantic retrieval, raw image-to-Gemini with no grounding, not measurable, not scalable. ContextIQ is the production backend that fixes every one of those problems.

---

## Architecture

```
User uploads PDF/DOCX/TXT
          ↓
FastAPI upload endpoint
          ↓
Celery worker (async, non-blocking)
          ↓
unstructured → parse document
          ↓
Chunking (semantic / sentence-window)
          ↓
OpenAI embeddings → Qdrant (cloud)
          ↓
[Document ready]

User asks question
          ↓
FastAPI query endpoint
          ↓
BM25 sparse retrieval ──┐
                        ├→ RRF fusion → Cohere Rerank → LlamaIndex QE → LLM
Dense retrieval ────────┘
          ↓
Streaming SSE response + sources + observability metadata
          ↓
React Web UI  /  Android Kotlin Client
```

---

## Major Features

- **Document Ingestion:** Upload PDF/DOCX/TXT, async Celery pipeline, unstructured parsing, chunking strategies
- **Hybrid Retrieval:** BM25 + Dense + RRF fusion + Cohere Rerank
- **Streaming Q&A:** SSE streaming with sources and observability metadata
- **RAGAs Evaluation:** 30-question test set, 3 configurations, 4 metrics — benchmark comparison table
- **Observability Dashboard:** Live per-query metrics (latency, scores, token usage), aggregate analytics
- **Cross-Platform:** One backend serving React web + Android Kotlin clients
- **Android App:** 14 screens, Jetpack Compose, Room DB offline cache, Retrofit networking

---

## External Services

| Service | Purpose | Auth Method |
|---|---|---|
| OpenAI | Embeddings (text-embedding-3-small) + LLM (GPT-4o-mini) | API key in .env |
| Qdrant Cloud | Vector storage and dense retrieval | API key + URL in .env |
| Cohere | Cross-encoder reranking | API key in .env |
| Redis (self-hosted) | Celery message broker + result backend | Docker container |
| Railway | Backend hosting | Account + Docker deployment |

---

## Dependencies

### Python (Backend)
fastapi, uvicorn, llama-index, llama-index-vector-stores-qdrant, qdrant-client, openai, cohere, rank-bm25, ragas, celery, redis, unstructured, python-dotenv, httpx, sse-starlette

### Node (Frontend)
react, recharts (planned)

### Android (Kotlin)
Jetpack Compose, Retrofit 2.9.0, Room DB, OkHttp, Coil

---

## Project Structure

```
contextiq/
├── .ai/                      ← Project memory
├── android/                  ← Android app (currently at repo root)
│   └── app/src/main/java/com/contextiq/app/
│       ├── data/             ← Room DB, Retrofit, Repository
│       ├── ui/               ← Screens, Components, Theme
│       └── utils/            ← OCRHelper, PdfUtils
├── backend/                  ← FastAPI backend (planned)
│   └── app/
│       ├── api/              ← Routes (documents, query, evaluation)
│       ├── core/             ← Config, Security
│       ├── ingestion/        ← Parser, Chunker, Celery tasks
│       ├── retrieval/        ← Dense, Sparse, Fusion, Reranker
│       ├── evaluation/       ← RAGAs pipeline, test set
│       └── models/           ← Pydantic schemas
├── frontend/                 ← React web app (planned)
│   └── src/
│       ├── components/       ← ChatWindow, DocumentUpload, etc.
│       └── pages/            ← Home, Dashboard
├── data/papers/              ← arxiv PDFs for testing
└── Ref/                      ← Reference documents
```
