# ContextIQ — Roadmap

---

## Legend

- ✅ Completed
- 🔄 In Progress
- ⏳ Planned
- ❌ Blocked

---

## Completed Milestones

### Android Migration (Scholium → ContextIQ)
- ✅ Renamed package `com.example.scholium` → `com.contextiq.app`
- ✅ Applied Meluko-inspired design language (Scholarly Navy, Clash Display, spring animations)
- ✅ Retrofit network layer with 13 API endpoints
- ✅ Deleted SarvamApiService (compromised key removed)
- ✅ Rewired all 14 screens to `ContextIQClient.api` singleton
- ✅ Theme renamed `Theme.Scholium` → `Theme.ContextIQ`
- ✅ Room DB schema updated (database name, package)
- ✅ Fonts added (Clash Display 6 weights)

### Day 0 — Backend Scaffold & API Keys ✅
- ✅ Python 3.12 venv with all dependencies installed
- ✅ `backend/.env` with working OpenAI + Cohere keys
- ✅ `test_connections.py` — all 3 services verified green
- ✅ `GET /api/v1/health` → 200 OK
- ✅ 5 arxiv PDFs in `data/papers/`

### Days 1–3 — Base RAG Pipeline ✅ (partial — LLM blocked)
- ✅ `ingestion/parser.py` — PDF/DOCX/TXT via pypdf
- ✅ `ingestion/chunker.py` — sentence_window + semantic
- ✅ `retrieval/dense.py` — Qdrant client + dynamic dim collection
- ✅ `retrieval/sparse.py` — BM25Retriever (empty-corpus bug fixed)
- ✅ `retrieval/fusion.py` — Reciprocal Rank Fusion (RRF_K=60)
- ✅ `retrieval/reranker.py` — Cohere Rerank cross-encoder
- ✅ `api/routes/documents.py` — upload + status endpoints
- ✅ `api/routes/query.py` — hybrid query + SSE stream
- ✅ `core/embeddings.py` — factory: fastembed (384d, local) / openai (1536d)
- ✅ `core/llm.py` — factory: groq (Llama 3.3 70B) / openai (GPT-4o-mini)
- ✅ First document upload: 76 chunks embedded via fastembed
- ❌ Full query with LLM answer — blocked on `GROQ_API_KEY`

---

## Current Milestone

### 🔄 Days 1–3 Completion (one blocker remaining)
- ❌ Add `GROQ_API_KEY` to `backend/.env` → unblocks LLM
- ⏳ Verify full `POST /api/v1/query` response (answer + sources + metadata)
- ⏳ Verify `POST /api/v1/query/stream` SSE token streaming

---

## Future Milestones

### ⏳ Days 3–5: Hybrid Search Polish
- Already implemented: BM25 + RRF + Cohere Rerank in query.py
- Verify all three configs work end-to-end (`_naive_rag`, `_dense_only`, full hybrid)
- Expose `config` param on `/api/v1/query` to switch configs per request

### ⏳ Days 6–7: Async Ingestion + Celery
- Wire up Celery + Redis broker for async document ingestion
- Move `_ingest_sync` to Celery task
- Status polling endpoint
- Docker Compose for local full-stack (FastAPI + Celery + Redis)

### ⏳ Days 8–9: RAGAs Evaluation
- `backend/app/evaluation/ragas_runner.py` — run faithfulness, answer_relevancy, context_precision, context_recall
- `data/eval/test_set.json` — 30 Q&A pairs across 5 arxiv papers
- Run 3 configs: Naive RAG, Dense-only, Hybrid+Rerank
- Comparison table with real numbers
- `POST /api/v1/evaluation/run` endpoint

### ⏳ Days 10–11: FastAPI Polish
- API key auth middleware
- Rate limiting
- Proper error handling + logging
- Pydantic v2 model improvements

### ⏳ Days 12–13: React Frontend
- Chat UI consuming SSE stream
- Document upload widget
- Observability dashboard (latency, scores)
- RAGAs benchmark table

### ⏳ Day 14: Deploy
- Dockerfile + docker-compose.yml
- Deploy to Railway
- Architecture diagram in README
- Loom demo recording

### ⏳ Days 15–19: Android Rewire
- Point Retrofit base URL to deployed Railway backend
- SSE streaming in `ChatDetailScreen`
- Test all 14 screens against live backend
- APK distribution

---

## Technical Debt

- Android project lives at repo root (should move to `android/` subdirectory)
- `PaperAnalyzerScreen.kt` uses fully qualified `com.contextiq.app.network.ContextIQClient` references instead of clean imports
- Room DB uses `fallbackToDestructiveMigration()` — needs versioned migrations
- Sarvam compromised key (`sk_59k2cw5q_...`) still needs rotation at Sarvam dashboard
- BM25 index rebuilt per query from dense results — not a true global index; acceptable for dev
- Qdrant `:memory:` loses data on server restart — needs persistent Qdrant for production

---

## Planned Improvements

- Add user authentication (JWT, beyond static API key)
- Support more document formats (HTML, Markdown)
- Batch document upload
- Persistent query history with per-query RAGAs scores
- Dark mode toggle on web frontend
- Android push notifications for ingestion completion
