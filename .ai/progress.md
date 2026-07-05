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

## 2026-07-02 — Scholium → ContextIQ Migration (Previous Work)

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

## 2026-07-02 — Day 0 Backend Scaffold

Set up the backend project structure and Python environment.

**Added:**
- `backend/` directory with full subpackage structure
- `backend/app/core/config.py` — Pydantic settings via .env
- `backend/app/main.py` — FastAPI app with health endpoint
- `backend/test_connections.py` — OpenAI + Qdrant + Cohere verification
- `backend/.env` (template and empty file)
- `backend/requirements.txt` — all dependencies frozen
- `data/papers/` directory for test PDFs

**Changed:**
- `.gitignore` — added backend/Python entries

---

## 2026-07-04 — Day 0 Completed + Day 1–3 Pipeline Verified ✅

**Verified:**
- OpenAI API: ✅ Connected (text-embedding-ada-002 listed)
- Qdrant (in-memory): ✅ Connected (0 collections, freshly created)
- Cohere API: ✅ Connected (chat response OK)
- FastAPI health: ✅ `GET /api/v1/health` → 200 OK `{"status":"ok","version":"0.1.0","model":"gpt-4o-mini"}`
- `data/papers/` — all 5 arxiv PDFs present:
  - `2302.00093_Weak-to-Strong_Generalization.pdf`
  - `2305.18290_QLoRA.pdf`
  - `2310.06825_Mixtral_of_Experts.pdf`
  - `2401.14295_TransNAR.pdf`
  - `2402.00161_RAG_for_LLMs.pdf`

**First end-to-end test:**
- Uploaded `2402.00161_RAG_for_LLMs.pdf` via `POST /api/v1/documents/upload`
- Parser: pypdf extracted text ✅
- Chunker: 76 sentence-window chunks ✅
- Embedder: OpenAI text-embedding-3-small → Qdrant in-memory ✅
- Query: `/api/v1/query` (Dense + BM25 + RRF + Cohere Rerank) ✅ (tested)
- SSE stream: `/api/v1/query/stream` ✅ (tested)

**Found and working:**
- `backend/app/ingestion/parser.py` — PDF/DOCX/TXT parser (pypdf)
- `backend/app/ingestion/chunker.py` — sentence_window + semantic
- `backend/app/retrieval/dense.py` — Qdrant retriever
- `backend/app/retrieval/sparse.py` — BM25Retriever
- `backend/app/retrieval/fusion.py` — RRF fusion
- `backend/app/retrieval/reranker.py` — Cohere Rerank
- `backend/app/api/routes/documents.py` — upload + status
- `backend/app/api/routes/query.py` — query + stream
- `backend/app/models/` — Pydantic schemas

**Known Issues:**
- `PaperAnalyzerScreen.kt` uses fully qualified `com.contextiq.app.network.ContextIQClient` references (not idiomatic imports)
- Room DB uses `fallbackToDestructiveMigration()` — needs proper migration
- Sarvam key still needs rotation at Sarvam dashboard
- BM25 index is rebuilt per-query (not persistent) — acceptable for now
- `evaluation/` module is empty — RAGAs pipeline not yet built
- Qdrant in-memory: data lost on server restart — need Cloud/persistent Qdrant for production

---

**Next milestone:** RAGAs evaluation pipeline (Days 8–9 in roadmap)
