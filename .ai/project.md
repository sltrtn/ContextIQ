# ContextIQ — Project Overview

> Living documentation for the ContextIQ project.

---

## What It Is

A production-grade RAG-powered document intelligence platform targeting the research paper domain. Users upload documents, ask questions, and get accurate sourced answers — with the system being evaluated, observable, and measurable, not just functional.

**Core thesis:** Most RAG projects are built to work. ContextIQ is built to be measured. LLM-as-judge evaluation across five retrieval configurations produces a comparison table with real numbers. Live faithfulness checking per query.

**Portfolio role:** AI credibility card — covers ML, Backend, and Android roles from a single project.

**Origin story:** Built Scholium (Kotlin Android research assistant, 12 screens, Gemini vision + Sarvam AI). Identified its core failure — no semantic retrieval, raw image-to-Gemini with no grounding, not measurable, not scalable. ContextIQ is the production backend that fixes every one of those problems.

---

## Architecture

```
User uploads PDF/DOCX/TXT
          ↓
FastAPI upload endpoint
          ↓
parse_document_pages() → page-level metadata
          ↓
Contextual chunking:
  1. Detect sections via regex
  2. LLM-summarize each section (1 call)
  3. Prepend [Section: name — summary] to each chunk
          ↓
fastembed (BAAI/bge-small-en-v1.5, 384d) → Qdrant
Global BM25 index built at ingestion
          ↓
[Document ready — 398 chunks from 5 papers]

User asks question
          ↓
FastAPI query endpoint (config parameter)
          ↓
Query expansion (optional, LLM generates 2-3 variants)
          ↓
Dense retrieval (Qdrant, top 20)
BM25 sparse retrieval (global index, top 20)  ──┐
                                                 ├→ RRF fusion (K=60)
                                                 │
Cohere Rerank (cross-encoder) ←──────────────────┘
          ↓
Context assembly:
  1. Dedup (SequenceMatcher, threshold 0.85)
  2. Lost-in-the-middle ordering
  3. Source labels: [1] filename.pdf (p.5): <chunk>
          ↓
LLM generation (Groq Llama 3.3 70B)
          ↓
Faithfulness post-check (claim extraction + verification)
          ↓
Response: answer + sources + metadata + faithfulness score
```

---

## Major Features

- **Contextual Chunking:** Section detection → LLM summarization → prepended context labels
- **Hybrid Retrieval:** BM25 + Dense + RRF fusion + Cohere Rerank
- **Query Expansion:** LLM generates 2-3 query variants for better recall
- **Context Assembly:** Dedup, lost-in-the-middle ordering, numbered source labels
- **5 Pipeline Configs:** vector_only, vector_rerank, hybrid, hybrid_rerank, long_context
- **Faithfulness Post-Check:** Claim extraction, context verification, unsupported claim details
- **LLM-as-Judge Evaluation:** 30-question test set, 4 metrics, 5 configurations
- **Streaming Q&A:** SSE streaming with sources and observability metadata
- **Cross-Platform:** One backend serving React web + Android Kotlin clients
- **Android App:** 14 screens, Jetpack Compose, Room DB offline cache, Retrofit networking

---

## External Services

| Service | Purpose | Auth Method |
|---|---|---|
| Groq | LLM (Llama 3.3 70B Versatile) — free tier | API key in .env |
| Cohere | Cross-encoder reranking (rerank-english-v3.0) | API key in .env |
| fastembed | Local embeddings (BAAI/bge-small-en-v1.5, 384d) | None (ONNX runtime) |
| Qdrant | Vector storage and dense retrieval (in-memory) | None (local) |
| OpenAI | Embeddings + LLM (fallback, requires billing) | API key in .env |

---

## Dependencies

### Python (Backend)
fastapi, uvicorn, llama-index, llama-index-vector-stores-qdrant, qdrant-client, fastembed, cohere, rank-bm25, groq, pydantic-settings, python-dotenv, httpx, pypdf

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
├── backend/
│   └── app/
│       ├── api/routes/       ← documents, query, evaluation
│       ├── core/             ← config, embeddings, llm
│       ├── ingestion/        ← parser, chunker
│       ├── retrieval/        ← dense, sparse, fusion, reranker, query_transform, context_assembly
│       ├── evaluation/       ← ragas_runner, configs, faithfulness
│       └── models/           ← query, document
├── data/
│   ├── papers/               ← 5 arxiv PDFs
│   └── eval/test_set.json    ← 30 Q&A pairs
├── frontend/                 ← React web app (planned)
└── Ref/                      ← Reference documents
```

---

## Query API

`POST /api/v1/query`

| Parameter | Type | Default | Description |
|---|---|---|---|
| `question` | string | required | The question to ask |
| `top_k` | int | 5 | Number of context chunks to use |
| `config` | string | `hybrid_rerank` | Pipeline config (see below) |
| `expand` | bool | false | Enable LLM query expansion |

**Configs:**
- `vector_only` — Dense retrieval, top_k
- `vector_rerank` — Dense + Cohere Rerank
- `hybrid` — Dense + BM25 + RRF, no reranking
- `hybrid_rerank` — Dense + BM25 + RRF + Cohere Rerank (full pipeline)
- `long_context` — Stuff all chunks into context, no retrieval
