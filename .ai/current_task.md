# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Backend build + evaluation complete. README, tests, Docker Compose, and retrieval metrics are done.**

Next: React frontend or deploy.

---

## What Works Right Now

- [x] `POST /api/v1/documents/upload` → contextual/sentence-window chunking → fastembed → Qdrant ✅
- [x] Dense retrieval (Qdrant + fastembed query) ✅
- [x] Global BM25 sparse retrieval (built at ingestion) ✅
- [x] RRF fusion ✅
- [x] Cohere Rerank with fallback + rate limiting ✅
- [x] LLM answer generation (Groq Llama 3.3 70B) ✅
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
- [ ] React frontend
- [ ] Deploy to Railway
- [ ] Full LLM-judge evaluation (needs paid tier)

---

## Next Steps (in order)

1. **[ ] React frontend** — chat UI with SSE streaming, document upload, observability dashboard
2. **[ ] Deploy to Railway** — Docker Compose + public URL
3. **[ ] Android rewire** — point Retrofit base URL to deployed backend
4. **[ ] Full LLM-judge evaluation** — run with paid Groq/OpenAI tier

---

## Start Commands

```bash
# Local backend
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate
uvicorn app.main:app --reload --port 8000

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
