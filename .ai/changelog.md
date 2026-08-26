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
- `core/llm.py` — `get_llm()`: groq (`openai/gpt-oss-120b`, free) or openai (GPT-4o-mini)
- Config: `EMBEDDING_PROVIDER`, `LLM_PROVIDER`, `GROQ_API_KEY`

**First upload tested:** 76 chunks embedded via fastembed ✅

**Bug fixed:** BM25 `ZeroDivisionError` on empty corpus.

**Committed + pushed:** `b686bae` to `origin/main`.

**Status:** Embedding + retrieval fully working. LLM blocked on `GROQ_API_KEY` being empty.

---

## 2026-07-06 — Bug Fixes + Groq Key + Full Pipeline

**Bug fixes (Phase 0):**
- `query.py` — replaced hardcoded `OpenAI(model=...)` with `get_llm()`
- `dense.py` — singleton pattern for in-memory Qdrant client
- `documents.py` — wrapped vector store in `StorageContext.from_defaults()` (critical: data was never reaching Qdrant)
- `reranker.py` — fallback uses input order instead of raw score sort

**Groq API key added:** LLM generation now works.

**Phase 0A — Global BM25:** Built once at ingestion, reused across queries.
**Phase 0C — Metadata enrichment:** Page numbers, filenames on chunks and Source objects.

---

## 2026-07-06 — Evaluation Pipeline (Phase 1)

**Test set:** `data/eval/test_set.json` — 30 Q&A pairs across 5 papers.

**Evaluation runner:** `evaluation/ragas_runner.py` — custom LLM-as-judge (no RAGAs dependency).
- Metrics: faithfulness, answer_relevancy, context_precision, context_recall

**5 pipeline configs:** vector_only, vector_rerank, hybrid, hybrid_rerank, long_context.

**Evaluation API:** `POST /api/v1/evaluation/run`, `GET /api/v1/evaluation/configs`.

---

## 2026-07-06 — Query Intelligence + Context Assembly (Phase 2)

**Contextual chunking:** Section detection → LLM summarization → prepended labels.
**Query rewriting:** LLM generates 2-3 query variants, retrieves for all, re-fuses.
**Context assembly:** Dedup (0.85 threshold), lost-in-the-middle ordering, source labels `[1] filename.pdf (p.5): ...`.

---

## 2026-07-06 — Ablation Isolation + Faithfulness (Phase 4-5)

**Config parameter:** `QueryRequest` has `config` (5 pipeline configs) and `expand` (query rewriting toggle).
**Faithfulness post-check:** Claim extraction, context verification, score + unsupported claims in response.
