# ContextIQ — Progress Log

> Chronological work log. Append entries as milestones are reached.

---

## 2026-07-02 — Initial Project Memory Setup

Created the `.ai/` project memory system and `AGENTS.md` with permanent project instructions.

**Added:**
- `.ai/project.md` — project overview, architecture, external services
- `.ai/roadmap.md` — completed milestones, current milestone, future milestones
- `.ai/current_task.md` — Day 0 setup task
- `.ai/progress.md` — this file
- `.ai/decisions.md` — architectural decisions log
- `.ai/changelog.md` — human-readable major changes
- `.ai/handoff.md` — session handoff for next agent
- `AGENTS.md` — permanent project instructions

---

## 2026-07-02 — Scholium → ContextIQ Migration

Migrated the entire Scholium Android app to ContextIQ with new design language and Retrofit networking.

**Added:**
- Retrofit network layer: `ContextIQApi.kt` (13 endpoints), `ContextIQClient.kt`, DTOs, `AuthInterceptor.kt`
- New design system: Scholarly Navy (#002855), Clash Display fonts, spring animations
- 14 redesigned screens with Retrofit integration
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- `ui/components/ExpressiveUtils.kt` with `pressScale()`
- `domain/UiState.kt` sealed class

**Changed:**
- Package: `com.example.scholium` → `com.contextiq.app`
- Theme: `Theme.Scholium` → `Theme.ContextIQ`
- App name: "Scholium" → "ContextIQ"
- Database: "scholium_database" → "contextiq_database"
- All 34 Kotlin files moved to new package

**Deleted:**
- `SarvamApiService.kt` (compromised API key removed)
- Old theme files under `com.example.scholium`
- Direct Gemini API calls (moved to backend)

**Security:**
- Removed hardcoded Sarvam API key `sk_59k2cw5q_rSbUWFbJ4OeexGxuE4g4IX4Z`
- All AI calls now go through backend — no keys on device

---

## 2026-07-02 — Day 0 Backend Scaffold

Set up the backend project structure and Python environment.

**Added:**
- `backend/` directory with full subpackage structure
- `backend/app/core/config.py` — Pydantic settings via .env
- `backend/app/main.py` — FastAPI app with health endpoint
- `backend/test_connections.py` — OpenAI + Qdrant + Cohere verification
- `backend/.env` (template and file)
- `backend/requirements.txt` — all dependencies frozen
- `data/papers/` directory for test PDFs

**Changed:**
- `.gitignore` — added backend/Python entries

---

## 2026-07-04 — Day 0 Complete + Days 1–3 Pipeline Built

**Day 0 verified:**
- OpenAI API ✅ — key working (no billing, but connection OK)
- Qdrant in-memory ✅
- Cohere API ✅
- `GET /api/v1/health` → 200 OK ✅
- All 5 arxiv PDFs confirmed in `data/papers/`

**Full RAG pipeline built and wired:**
- `backend/app/ingestion/parser.py` — PDF/DOCX/TXT via pypdf
- `backend/app/ingestion/chunker.py` — sentence_window + semantic
- `backend/app/retrieval/dense.py` — Qdrant client + dynamic-dim collection setup
- `backend/app/retrieval/sparse.py` — BM25Retriever (rank_bm25)
- `backend/app/retrieval/fusion.py` — Reciprocal Rank Fusion
- `backend/app/retrieval/reranker.py` — Cohere cross-encoder rerank
- `backend/app/api/routes/documents.py` — upload + status endpoints
- `backend/app/api/routes/query.py` — hybrid query + SSE stream (3 internal configs)
- `backend/app/models/document.py` + `query.py` — Pydantic schemas

**Provider factories added (no hardcoded API dependency):**
- `backend/app/core/embeddings.py` — `get_embed_model()`: fastembed (384d local) or openai
- `backend/app/core/llm.py` — `get_llm()`: groq (Llama 3.3 70B free) or openai
- `EMBEDDING_PROVIDER`, `LLM_PROVIDER`, `GROQ_API_KEY` added to config + .env.template

**First upload test:**
- `2402.00161_RAG_for_LLMs.pdf` → 76 sentence-window chunks → fastembed → Qdrant ✅

**Bugs fixed:**
- BM25 `ZeroDivisionError` on empty corpus — guard added in `sparse.py`

**Committed:** `b686bae` — pushed to `origin/main`

---

## 2026-07-06 — .ai docs updated + commit pushed

All changes from the 2026-07-04 session committed and pushed.

**Commit:** `b686bae` — 15 files, 341 insertions
**Repo URL:** https://github.com/sltrtn/ContextIQ

**Current blocker:** `GROQ_API_KEY` is empty — LLM generation not yet tested.

**Known issues at this point:**
- `PaperAnalyzerScreen.kt` fully-qualified package references (not idiomatic)
- Room DB `fallbackToDestructiveMigration()` — needs proper migration
- Sarvam key still needs rotation at Sarvam dashboard
- BM25 is per-query (not persistent global index) — acceptable for now
- Qdrant in-memory loses data on server restart — re-upload required each session
- `backend/app/evaluation/` is empty stub — RAGAs not yet implemented

---

## 2026-07-06 — Phase 0: Bug Fixes + Groq Key

**Fixed critical bugs:**
- `query.py:118,190` — replaced hardcoded `OpenAI(model=...)` with `get_llm()`
- `dense.py` — singleton pattern for in-memory Qdrant client
- `documents.py` — wrapped vector store in `StorageContext.from_defaults()` (data was never reaching Qdrant)
- `reranker.py` — fallback now uses input order instead of raw score sort

**Added Groq API key:** Set in `backend/.env` (see credentials manager)

**Phase 0A — Global BM25:** `sparse.py` has `build_global_bm25()` + `get_global_bm25()` singleton
**Phase 0B — Reranker fallback fixed**
**Phase 0C — Metadata enrichment:** `parse_document_pages()`, page numbers in chunks, `filename`/`page` on Source model

---

## 2026-07-06 — Phase 1: Evaluation Pipeline

**Test set:** `data/eval/test_set.json` — 30 Q&A pairs (6 per paper, mix of types/difficulty)

**Evaluation runner:** `evaluation/ragas_runner.py` — custom LLM-as-judge (no RAGAs dependency needed)
- Metrics: faithfulness, answer_relevancy, context_precision, context_recall

**Evaluation API:** `api/routes/evaluation.py` — `POST /api/v1/evaluation/run`, `GET /api/v1/evaluation/configs`

**5 pipeline configs:** `evaluation/configs.py` — vector_only, vector_rerank, hybrid, hybrid_rerank, long_context

**Smoke test passed:** Ingested 69 chunks, queried "What is QLoRA?", got answer + 5 contexts

---

## 2026-07-06 — Phase 2: Query Intelligence + Context Assembly

**Contextual chunking (Phase 2A):** `chunker.py` — `contextual_chunker()` detects sections via regex, summarizes via LLM, prepends `[Section: name — summary]` to each chunk. Tested: 3 sections detected in QLoRA paper, 70 chunks produced.

**Query rewriting (Phase 2B):** `query_transform.py` — `expand_query()` generates 2-3 query variants via LLM, retrieves for all, re-fuses with RRF.

**Context assembly (Phase 2C):** `context_assembly.py` — dedup (SequenceMatcher, threshold 0.85), lost-in-the-middle ordering, source labeling (`[1] filename.pdf (p.5): ...`).

**Ablation isolation (Phase 4):** `QueryRequest` now has `config` and `expand` parameters. 5 configs work independently. Evaluation runner passes config to query function.

---

## 2026-07-06 — Phase 5: Faithfulness Post-Check

**Faithfulness checker:** `evaluation/faithfulness.py` — post-generation LLM-as-judge that extracts claims, verifies each against contexts, returns score + unsupported claims list.

**Wired into `/query`:** Response now includes `faithfulness` field with score, supported/total claims, and unsupported claim details.

**Tested:** Correctly caught hallucination (Google/8-bit claim → 0.0 score), rewarded grounded answers.
