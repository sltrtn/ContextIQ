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
- `backend/app/core/llm.py` — `get_llm()`: groq (`openai/gpt-oss-120b` free) or openai
- `EMBEDDING_PROVIDER`, `LLM_PROVIDER`, `GROQ_API_KEY` added to config + .env.template

**First upload test:**
- `2402.00161_QKD.pdf` → 76 sentence-window chunks → fastembed → Qdrant ✅

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

**Bug fixes (Phase 0):**
- `query.py` — replaced hardcoded `OpenAI(model=...)` with `get_llm()`
- `dense.py` — singleton pattern for in-memory Qdrant client
- `documents.py` — wrapped vector store in `StorageContext.from_defaults()` (critical: data was never reaching Qdrant)
- `reranker.py` — fallback uses input order instead of raw score sort
- Global BM25 built once at ingestion, reused across queries
- Metadata enrichment: page numbers, filenames on chunks and Source objects

---

## 2026-07-06 — Phase 1: Evaluation Pipeline

**Test set:** `data/eval/test_set.json` — 30 Q&A pairs across 5 papers

**Evaluation runner:** `evaluation/ragas_runner.py` — custom LLM-as-judge (no RAGAs dependency)
- Metrics: faithfulness, answer_relevancy, context_precision, context_recall

**5 pipeline configs:** `evaluation/configs.py` — vector_only, vector_rerank, hybrid, hybrid_rerank, long_context

**Evaluation API:** `api/routes/evaluation.py` — `POST /api/v1/evaluation/run`, `GET /api/v1/evaluation/configs`

---

## 2026-07-06 — Phase 2: Query Intelligence + Context Assembly

**Contextual chunking (Phase 2A):** `chunker.py` — `contextual_chunker()` detects sections via regex, summarizes via LLM, prepends `[Section: name — summary]` to each chunk

**Query rewriting (Phase 2B):** `query_transform.py` — `expand_query()` generates 2-3 query variants via LLM, retrieves for all, re-fuses

**Context assembly (Phase 2C):** `context_assembly.py` — dedup, lost-in-the-middle ordering, source labels

---

## 2026-07-06 — Phase 4-5: Ablation Isolation + Faithfulness

**Config parameter:** `QueryRequest` has `config` (5 pipeline configs) and `expand` (query rewriting toggle)

**Faithfulness post-check:** `evaluation/faithfulness.py` — claim extraction, context verification, score + unsupported claims

---

## 2026-08-03 — Phase 6: README, Tests, Retrieval Metrics, Docker

**Data integrity:** Renamed 4 PDFs to match actual content; updated `test_set.json` and docs

**pytest suite:** `backend/tests/` with 39 passing tests
- parser, chunker with mock LLM, BM25, RRF fusion, reranker fallback, context assembly, query transform fallback, retrieval metrics

**Code fixes from tests:**
- `query_transform.py`: moved `get_llm()` inside try block for fallback
- `reranker.py`: wrapped `get_cohere_client()` inside try block, preserve all metadata, added rate-limiting guard for Cohere trial key
- `fusion.py`: preserve all metadata from input items
- `sparse.py`: preserve all chunk metadata, prefer `chunk_id` over `node_id`

**Retrieval metrics:** `evaluation/retrieval_metrics.py` + `run_retrieval_metrics.py`
- P@5, R@5, MRR across 30 questions × 5 configs
- Real numbers saved to `data/eval/retrieval_metrics.json`
- Key finding: vector_rerank wins, hybrid is strong, hybrid_rerank underperforms (reranker hurts on top of fusion)

**Docker Compose:** `docker-compose.yml` + `Dockerfile` for Qdrant + backend

**README:** architecture diagram, stack, API examples, eval table, PDF mapping, getting started

**Known blockers:**
- Full LLM-judge evaluation blocked by Groq free-tier daily token limit (100k/day)
- Cohere trial key limited to 10 calls/minute (reranker auto-throttles)

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

---

## 2026-08-20 — Documentation Sweep + Frontend Commit + Quick Fixes

**Model name bulk update:**
- Replaced every functional reference to retired `llama-3.3-70b-versatile` with `openai/gpt-oss-120b`
- Updated: `README.md`, `AGENTS.md`, `.ai/*.md`, `Ref/interview/*`, `Ref/portfolio/ContextIQ-LinkedIn-Post.md`, `backend/.env.template`, `backend/app/core/llm.py`
- Historical notes kept in `.ai/decisions.md` and `.ai/audit.md` for context

**Port standardization:**
- Local dev references updated from `8000` to `8001` (README, AGENTS, `.ai/`, Android `build.gradle.kts`)
- Port 8000 remains blocked by orphaned socket; Docker Compose internal mapping still uses 8000:8000

**Quick fixes:**
- `backend/app/evaluation/faithfulness.py`: context truncation 500 → 2000 chars
- `README.md`: test count 33 → 39

**Frontend committed:**
- Added `frontend/` source files to repo (React + Vite, black-and-white design)
- `.gitignore` updated to exclude `node_modules/`, `dist/`, and Vite build artifacts

**Android / design system committed:**
- Added `ContextIQDesign.kt` design tokens
- Updated `Theme.kt` to disable dynamic color
- Updated `ExpressiveUtils.kt` to use `ContextIQDesign.Motion.ButtonPressScale`
- Added cross-platform design contract to `.ai/design-system.md`

**Commits pushed:**
- `3ef3efb` fix(backend,android): switch Groq model to openai/gpt-oss-120b, widen faithfulness context, update Android base URL
- `8ccc9b0` docs: bulk-update model name and port across all docs
- `90b783b` feat(android,design): add ContextIQDesign tokens, cross-platform design contract, deterministic theme
- `e89e849` feat(frontend): add React/Vite frontend
- `8d21634` chore(gitignore): ignore frontend build artifacts

**Closed in this session:**
- Android endpoints now aligned with backend (only Paper Analyzer fully wired; other screens stubbed honestly)

**Still open (biggest blockers):**
- Qdrant `:memory:` loses data on restart
- Cohere trial rate limit makes evaluation slow
- No API auth / rate limiting
- Not deployed to Railway

---

## 2026-08-19 — Interview Prep + Portfolio Sync

**Model display fix:**
- `config.py` — added `active_llm_model` and `active_embedding_model` properties
- `query.py` — response metadata now reports the actual model in use
- `main.py` — startup log and `/health` report the actual model
- Prevents the bug where Groq `openai/gpt-oss-120b` runs but the API claims `gpt-4o-mini`

**Interview quick sheet:**
- Created `Ref/interview/ContextIQ-Interview-Quick-Sheet.md`
- TCS Prime-style last-minute revision guide: project pitch, 3 challenge stories, tech table, RAG fundamentals, design decisions, behavioral answers, rapid-fire recall
- Includes real eval numbers from `data/eval/retrieval_metrics.json`

**Commits pushed:**
- `8518ebe` fix(backend): report active LLM model instead of hardcoded openai_model
- `2067458` docs(interview, .ai): add ContextIQ TCS Prime interview quick sheet

**Current focus:**
- Portfolio storytelling (resume, LinkedIn post, eval screenshots)
- Mock interview drills using the quick sheet
- Pending: React frontend / Railway deploy / full LLM-judge eval (needs paid tier)

## 2026-08-19 — LinkedIn Portfolio Draft

**Added:** `Ref/portfolio/ContextIQ-LinkedIn-Post.md`

- Ready-to-post LinkedIn copy built around the measured finding that `vector_rerank` outperformed `hybrid_rerank` (P@5 0.9933 vs 0.8533).
- Explicitly scopes the result as retrieval-only, 30 questions × 5 papers, with paper-level relevance as the proxy; avoids overstating it as LLM-judge or human passage-level evidence.
- Added safe live-query screenshot instructions, including a redaction/crop checklist.

**Blocker:** Could not create the requested real live-server screenshot in this environment. Port 8001 is already reserved but has no reachable listener, and the local `fastembed` model is not cached while outbound Hugging Face access fails DNS resolution. No synthetic screenshot was created.

## 2026-08-19 — React Frontend Visual Prototype

**Added:** `frontend/` — minimal Vite + React + TypeScript app.

- Built a pure black-and-white, Outfit-only research interface for ContextIQ.
- Includes document dropzone, query composer, three selectable pipeline modes, animated ask action, response / faithfulness layout, and source cards.
- Uses transform, clipping, layout, and border animations for hover/click feedback without adding non-black/white colors.
- It is intentionally a client-side prototype. Upload/query API calls remain unwired until a reachable backend is available; no credentials were added.

## 2026-08-19 — React API Wiring

- Replaced mocked upload/query interaction with real `FormData` upload to `/api/v1/documents/upload` and JSON query requests to `/api/v1/query`.
- The UI now renders the API answer, model, latency, faithfulness score, source names and pages; each result is request-specific.
- Added upload/query loading states, API error display, and a Vite development proxy from `/api` to `http://127.0.0.1:8001`.
- Full browser-to-backend validation remains blocked until port 8001 and its locally cached embedding model are available.

## 2026-08-20 — ContextIQ Cross-Platform Design Language

- Audited the Android `Color.kt`, `Theme.kt`, `Type.kt`, shared Compose components, and all screen-level styling patterns against the React frontend.
- Added a binding ContextIQ Android/web contract to `.ai/design-system.md`: Outfit, shared spacing/radius/control tokens, flat evidence cards, numbered workflow, answer anatomy, and interaction/state rules.
- Added `app/.../ui/theme/ContextIQDesign.kt` as the Android token source and linked the standard `pressScale()` default to it.
- Changed `ContextIQTheme` to disable Material dynamic color by default, so the authored Scholarly Navy brand is not replaced by device wallpaper colors.
- Added matching semantic spacing/radius tokens to the React stylesheet while preserving its strict `#000`/`#fff` palette.
- Existing Android screens contain historical raw dp/radius values; migrate them incrementally with device QA rather than mass-changing 14 screens without visual verification.

---

## 2026-08-20 — Android-Backend Endpoint Alignment

**Problem:** Android app called endpoints from the old Scholium backend (`/analyze/*`, `/tools/*`, `/chat/stream`) that did not exist in the ContextIQ FastAPI backend.

**Solution:**
- Rewrote `app/.../network/ContextIQApi.kt` to expose only real backend endpoints:
  - `POST api/v1/documents/upload`
  - `POST api/v1/query`
  - `POST api/v1/query/stream`
  - `GET api/v1/health`
- Added `app/.../network/dto/BackendDto.kt` with `UploadResponse`, `QueryRequest`, `QueryResponse`, `QuerySourceDto`, `FaithfulnessCheckDto`, `QueryMetadataDto`.
- Rewired `PaperAnalyzerScreen`:
  - PDF picker uploads to `/documents/upload`
  - Chat sends messages to `/query` (non-streaming, `hybrid_rerank`, top_k=5)
  - Response shows answer + source filenames/pages + faithfulness score
  - Messages and sessions saved to Room DB as before
- Stubbed 9 unsupported screens with honest error messages:
  - AbstractSummary, Citation, ClaimVerifier, JournalMatcher, LatexGenerator, LitReviewer, OpenAccess, PaperReviewer, RelatedPapers, ReviewRebuttal
  - Each now explains that the feature is not supported and directs the user to Paper Analyzer.

**Build verification:**
- `./gradlew :app:compileDebugKotlin` passes with only deprecation warnings.
- Full `assembleDebug` fails at `compileDebugJavaWithJavac` due to environment JDK 26 being incompatible with Android SDK 35 `jlink` step. This is a machine-level JDK issue, not a code issue. To fully build the APK, switch to JDK 17 or 21.

**Commits:**
- `d5d997e` fix(android): align ContextIQApi with backend endpoints; wire Paper Analyzer to upload+query; stub unsupported screens
