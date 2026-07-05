# Changelog

> Human-readable summary of major repository changes. Not every commit — only meaningful milestones.

---

## 2026-07-02 — Repository Created

Forked from Scholium (`arnavt1605/Scholium`). Initial commit with ContextIQ branding and all migrated Android code.

---

## 2026-07-02 — Project Memory System

**Created:** `.ai/` directory with full project memory: project.md, roadmap.md, current_task.md, progress.md, decisions.md, handoff.md, changelog.md.
**Created:** `AGENTS.md` with permanent project instructions.

---

## 2026-07-02 — Scholium → ContextIQ Migration

**Package rename:** `com.example.scholium` → `com.contextiq.app`.
**App rename:** "Scholium" → "ContextIQ".
**Design language:** Meluko-inspired with Scholarly Navy (#002855) hero color, Clash Display fonts, spring animations, rounded corners.
**Networking:** Retrofit layer with 13 API endpoints replacing direct OkHttp calls.
**Security:** Removed hardcoded Sarvam API key. All AI calls now go through backend.
**Screens:** All 14 screens rewired to `ContextIQClient.api` singleton.
**Theme:** `Theme.ContextIQ` with dark/light schemes.

---

## 2026-07-02 — Day 0 Backend Scaffold

**Created:** `backend/` directory with FastAPI app skeleton, Pydantic config, connection test script, .env template, requirements.txt.
**Updated:** `.gitignore` for backend artifacts.

---

## 2026-07-04–06 — Full RAG Pipeline (commit b686bae)

**Day 0 verified:** OpenAI ✅, Qdrant ✅, Cohere ✅, FastAPI health ✅, all 5 PDFs present.

**Pipeline built:**
- `ingestion/`: parser (pypdf), chunker (sentence_window + semantic)
- `retrieval/`: dense (Qdrant), sparse (BM25), fusion (RRF), reranker (Cohere)
- `api/routes/`: documents (upload/status), query (hybrid + SSE stream)
- `models/`: document + query Pydantic schemas

**Provider factories** (switch between free-tier and paid without code changes):
- `core/embeddings.py` — `get_embed_model()`: fastembed (local ONNX, 384d, free) or openai (1536d)
- `core/llm.py` — `get_llm()`: groq (Llama 3.3 70B, free) or openai (GPT-4o-mini)
- Config: `EMBEDDING_PROVIDER`, `LLM_PROVIDER`, `GROQ_API_KEY`

**First upload tested:** 76 chunks embedded via fastembed ✅

**Bug fixed:** BM25 `ZeroDivisionError` on empty corpus.

**Committed + pushed:** `b686bae` to `origin/main`.

**Status:** Embedding + retrieval fully working. LLM blocked on `GROQ_API_KEY` being empty.
