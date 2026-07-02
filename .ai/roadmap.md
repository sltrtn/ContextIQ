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

---

## Current Milestone

### 🔄 Day 0 — Backend Scaffold & API Keys
- Set up Python environment (venv, requirements.txt)
- Create `.env` with API keys
- `test_connections.py` verifying OpenAI, Qdrant Cloud, Cohere
- Docker Compose scaffold (FastAPI + Redis + Celery)
- Register accounts: OpenAI, Cohere, Qdrant Cloud, Railway

---

## Future Milestones

### ⏳ Days 1–3: Base RAG Pipeline
- LlamaIndex + Qdrant + OpenAI integration
- Single document ingestion flow
- Basic query end-to-end (upload → ask → answer)
- FastAPI app with `/api/v1/health` endpoint

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
