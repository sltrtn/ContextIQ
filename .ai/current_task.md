# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Backend build + evaluation complete. README, tests, Docker Compose, retrieval metrics, React frontend, interview prep docs, and Android-backend alignment are done.**

Recently completed (this session):
- Rewrote Android `ContextIQApi.kt` to use real backend endpoints: `/api/v1/documents/upload`, `/api/v1/query`, `/api/v1/query/stream`
- Added backend-aligned DTOs (`BackendDto.kt`)
- Rewired `PaperAnalyzerScreen` to upload PDFs and chat via `/query`
- Stubbed unsupported screens with honest "not supported by current backend" messages
- Verified Kotlin compilation passes (full APK build blocked only by environment JDK 26 vs Android SDK 35)

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
- [ ] Deploy to Railway
- [ ] Full LLM-judge evaluation (needs paid tier)

## Portfolio status (2026-08-19)

- [x] LinkedIn draft created at `Ref/portfolio/ContextIQ-LinkedIn-Post.md`; it accurately frames the retrieval-only 30-question × 5-paper evaluation.
- [ ] Live-query screenshot pending: this environment cannot bind a reachable port 8001 and does not have the fastembed model cached; outbound Hugging Face access is unavailable. The draft includes the safe capture command and crop guidance.

## Frontend status (2026-08-20)

- [x] React/Vite frontend added at `frontend/`: black-and-white Outfit interface with upload, query composer, pipeline selector, response/sources and keyboard/click interactions.
- [x] Upload and query controls wired to FastAPI (`/documents/upload`, `/query`); Vite proxies `/api` to local port 8001 by default.
- [ ] Commit `frontend/` source files to repo (exclude `node_modules/` and `dist/`)
- [ ] Validate the full browser flow against a reachable backend with the embedding model available.

## Design system status (2026-08-20)

- [x] Audited Android theme, typography, shared components, and all Compose screen styling alongside the React frontend.
- [x] Added the binding ContextIQ cross-platform contract to `.ai/design-system.md`.
- [x] Added Android `ContextIQDesign` tokens and made the ContextIQ brand theme deterministic (`dynamicColor = false`).
- [x] Added matching semantic layout tokens to the black-and-white web frontend without changing its pure black/white palette.
- [ ] Incrementally migrate existing Android screens from raw dp/radius literals to `ContextIQDesign` as they are next edited; do not do a risky all-screen visual rewrite without device QA.

---

## Next Steps (in order)

1. **[ ] Deploy to Railway** — Docker Compose + public URL
2. **[ ] Point Android + frontend to deployed URL** — update base URL from `10.0.2.2:8001` to public Railway URL
3. **[ ] Full LLM-judge evaluation** — run with paid Groq/OpenAI tier
4. **[ ] Re-implement unsupported Android screens** — only if needed for portfolio; currently stubbed with honest messages

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
