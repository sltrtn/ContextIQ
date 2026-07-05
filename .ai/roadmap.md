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

### Day 0 — Backend Scaffold & API Keys ✅ COMPLETE
- ✅ Python venv with all dependencies
- ✅ `.env` with working API keys (OpenAI, Cohere, Qdrant in-memory)
- ✅ `test_connections.py` — all 3 services verified
- ✅ FastAPI health endpoint 200 OK
- ✅ 5 arxiv PDFs in `data/papers/`

### Days 1–3 — Base RAG Pipeline ✅ COMPLETE (pending Groq key)
- ✅ `ingestion/parser.py` — PDF/DOCX/TXT parsing
- ✅ `ingestion/chunker.py` — sentence_window + semantic
- ✅ `retrieval/dense.py` — Qdrant + embedding model factory
- ✅ `retrieval/sparse.py` — BM25Retriever (rank_bm25, bug fixed)
- ✅ `retrieval/fusion.py` — Reciprocal Rank Fusion
- ✅ `retrieval/reranker.py` — Cohere Rerank
- ✅ `api/routes/documents.py` — upload + status endpoints
- ✅ `api/routes/query.py` — hybrid query + SSE stream
- ✅ `core/embeddings.py` — embedding factory (fastembed / openai)
- ✅ `core/llm.py` — LLM factory (groq / openai)
- ✅ First document upload: 76 chunks embedded ✅
- ⏳ Full query test: waiting for Groq API key

---

## Current Milestone

### ⏳ Days 3–5: Hybrid Search
- BM25 sparse retriever
- RRF fusion
- Hybrid scores visible in query response

### ⏳ Days 6–7: Rerank + Async Ingestion
- Cohere Rerank wrapper
- Celery async ingestion pipeline
- Docker Compose full stack running
- Full pipeline end-to-end

### ⏳ Days 8–9: RAGAs Evaluation
- 30-question test set on 5–10 arxiv papers
- Three configurations benchmarked (Naive RAG, Dense Only, Hybrid + Rerank)
- Comparison table with real numbers

### ⏳ Days 10–11: FastAPI Polish
- All endpoints (documents, query, evaluation)
- SSE streaming with metadata
- API key middleware + rate limiting
- Pydantic models for all schemas

### ⏳ Days 12–13: React Frontend
- Chat UI with SSE consumption
- Document upload widget
- Observability dashboard + metrics table
- Recruiter-demo-ready web app

### ⏳ Day 14: Deploy
- Docker Compose → Railway
- Architecture diagram in README
- Loom demo recording

### ⏳ Days 15–19: Android Rewire
- Point Retrofit to deployed backend
- SSE streaming in ChatDetailScreen
- Test all 14 screens against live backend
- APK distribution

---

## Technical Debt

- Android project at repo root (should move to `android/`)
- `PaperAnalyzerScreen.kt` uses fully qualified package references instead of imports
- Room DB uses `fallbackToDestructiveMigration()` — needs proper migration
- Sarvam compromised key must be rotated at Sarvam dashboard

---

## Planned Improvements

- Add user authentication (beyond API key)
- Support for more document formats (HTML, MD)
- Batch document upload
- Query history with persistent RAGAs scoring
- Dark mode toggle on web frontend
- Android push notifications for ingestion completion
