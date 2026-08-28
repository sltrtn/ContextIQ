# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Android app redesigned to match the web client's black-and-white editorial language.** Backend, tests, React frontend, docs, and backend alignment are done.

Recently completed (this session):
- Created `LandingScreen` with web-client wordmark, tagline "RESEARCH, INTERROGATED.", and marquee language.
- Created `WorkbenchScreen` as the single primary flow: 01/SOURCE upload, 02/INTERROGATE query, 03/METHOD pipeline selector, 04/RESPONSE with answer, faithfulness, sources.
- Restyled `HistoryScreen` and `ChatDetailScreen` with the same black/white, uppercase, numbered vocabulary.
- Replaced `MainActivity` navigation graph: `landing` → `workbench` → `history` → `chat_detail`.
- Removed the 12 old screens and `ChatSheet` that no longer fit the focused interrogation flow.
- Forced `ContextIQTheme(darkTheme = true)` so Android mirrors the web client's `#000`/`#fff` palette.
- Verified Kotlin compilation passes (`./gradlew :app:compileDebugKotlin`).

Next focus: deploy backend so Android + frontend can hit a live URL.

---

## What Works Right Now

- [x] `POST /api/v1/documents/upload` → contextual/sentence-window chunking → fastembed → Qdrant ✅
- [x] Dense retrieval (Qdrant + fastembed query) ✅
- [x] Global BM25 sparse retrieval (built at ingestion) ✅
- [x] RRF fusion ✅
- [x] Cohere Rerank with fallback + rate limiting ✅
- [x] LLM answer generation (Groq openai/gpt-oss-120b) ✅
- [x] Query expansion (LLM generates 2-3 variants) ✅
- [x] Context assembly (dedup, ordering, source labels) ✅
- [x] Faithfulness post-check (claim extraction + verification) ✅
- [x] 5 pipeline configs (vector_only, vector_rerank, hybrid, hybrid_rerank, long_context) ✅
- [x] LLM-as-judge evaluation runner ✅
- [x] 30-question test set ✅
- [x] Retrieval-only metrics (P@5, R@5, MRR) across 30 questions × 5 configs ✅
- [x] pytest suite (39 tests passing) ✅
- [x] Docker Compose with persistent Qdrant ✅
- [x] README with architecture + eval table ✅
- [x] All docs updated to current model (`openai/gpt-oss-120b`) ✅
- [x] React frontend built ✅
- [x] Faithfulness context window increased ✅
- [x] Android-backend endpoint alignment ✅
- [x] Android visual redesign to match web client ✅
- [ ] Deploy to Railway
- [ ] Full LLM-judge evaluation (needs paid tier)

## Portfolio status (2026-08-19)

- [x] LinkedIn draft created at `Ref/portfolio/ContextIQ-LinkedIn-Post.md`; it accurately frames the retrieval-only 30-question × 5-paper evaluation.
- [ ] Live-query screenshot pending: this environment cannot bind a reachable port 8001 and does not have the fastembed model cached; outbound Hugging Face access is unavailable. The draft includes the safe capture command and crop guidance.

## Frontend status (2026-08-20)

- [x] React/Vite frontend added at `frontend/`: black-and-white Outfit interface with upload, query composer, pipeline selector, response/sources and keyboard/click interactions.
- [x] Upload and query controls wired to FastAPI (`/documents/upload`, `/query`); Vite proxies `/api` to local port 8001 by default.
- [x] `frontend/` source files committed to repo (`node_modules/` and `dist/` excluded).
- [ ] Validate the full browser flow against a reachable backend with the embedding model available.

## Design system status (2026-08-28)

- [x] Audited Android theme, typography, shared components, and all Compose screen styling alongside the React frontend.
- [x] Added the binding ContextIQ cross-platform contract to `.ai/design-system.md`.
- [x] Added Android `ContextIQDesign` tokens and made the ContextIQ brand theme deterministic (`dynamicColor = false`).
- [x] Android app now uses the web client's black/white editorial language: "CONTEXTIQ" wordmark, "RESEARCH, INTERROGATED.", numbered workflow (01/SOURCE, 02/INTERROGATE, 03/METHOD, 04/RESPONSE), high-contrast cards, and Outfit typography.
- [x] Removed old feature-specific screens that did not match the focused interrogation flow.

---

## Next Steps (in order)

1. **[ ] Deploy to Railway** — Docker Compose + public URL
2. **[ ] Point Android + frontend to deployed URL** — update base URL from `10.0.2.2:8001` to public Railway URL
3. **[ ] Full LLM-judge evaluation** — run with paid Groq/OpenAI tier
4. **[ ] Re-implement extended Android features** — only if needed for portfolio; current flow is upload + query + history

---

## Start Commands

```bash
# Local backend
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate
uvicorn app.main:app --reload --port 8001

# Docker Compose
cd /home/mad/StudioProjects/ContextIQ/backend
docker compose up --build

# Tests
cd /home/mad/StudioProjects/ContextIQ/backend
pytest tests/ -v

# Retrieval metrics
cd /home/mad/StudioProjects/ContextIQ/backend
python run_retrieval_metrics.py
```
