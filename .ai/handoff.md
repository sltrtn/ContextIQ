# Handoff — Session End

> **Last updated:** 2026-07-06
> **Last action:** Committed + pushed all pipeline code (commit b686bae). Groq key still missing.

---

## Current Status

| Component | State |
|---|---|
| FastAPI server | ✅ runs (`uvicorn app.main:app --reload` in `backend/`) |
| Document upload | ✅ working — 76 chunks embedded in test |
| fastembed (BAAI/bge-small-en-v1.5, 384d) | ✅ installed, zero cost |
| Qdrant in-memory | ✅ works, **data lost on server restart** |
| Cohere Rerank | ✅ working |
| BM25 + RRF fusion | ✅ working |
| Groq LLM (Llama 3.3 70B) | ❌ **BLOCKED — `GROQ_API_KEY=` is empty** |
| Full `/api/v1/query` response | ❌ blocked by Groq key |
| RAGAs evaluation | ⏳ not started |
| React frontend | ⏳ not started |
| Railway deploy | ⏳ not started |

---

## What Was Built (full backend stack)

### Core infrastructure
- `backend/app/main.py` — FastAPI app, lifespan, health endpoint
- `backend/app/core/config.py` — Pydantic settings, `.env` loading, `embedding_dim` property
- `backend/app/core/embeddings.py` — factory: `get_embed_model()` → fastembed or openai
- `backend/app/core/llm.py` — factory: `get_llm()` → groq or openai

### Ingestion pipeline
- `backend/app/ingestion/parser.py` — PDF (pypdf), DOCX (python-docx), TXT
- `backend/app/ingestion/chunker.py` — `sentence_window_chunker()` + `semantic_chunker()`
- `backend/app/ingestion/tasks.py` — Celery task scaffold (not active without Redis)

### Retrieval pipeline
- `backend/app/retrieval/dense.py` — `get_qdrant_client()`, `ensure_collection()` (dynamic dim)
- `backend/app/retrieval/sparse.py` — `BM25Retriever` with empty-corpus guard
- `backend/app/retrieval/fusion.py` — `reciprocal_rank_fusion()` (RRF_K=60)
- `backend/app/retrieval/reranker.py` — Cohere `rerank-english-v3.0`

### API routes
- `backend/app/api/routes/documents.py` — `POST /api/v1/documents/upload`, `GET /api/v1/documents/{id}/status`
- `backend/app/api/routes/query.py` — `POST /api/v1/query` (hybrid+rerank), `POST /api/v1/query/stream` (SSE), `_naive_rag()`, `_dense_only()` helpers

### Models
- `backend/app/models/document.py` — `UploadResponse`, `DocumentStatus`, `DocumentListItem`
- `backend/app/models/query.py` — `QueryRequest`, `QueryResponse`, `Source`

### Empty stubs (to be built)
- `backend/app/evaluation/` — only `__init__.py` exists, no ragas_runner yet
- `backend/app/api/routes/` — no evaluation route yet

---

## Repo State

- **Branch:** `main`
- **Latest commit:** `b686bae` — `feat(backend): Day 0+1-3 complete — full RAG pipeline live with fastembed + Groq`
- **Remote:** `https://github.com/sltrtn/ContextIQ`
- **Working tree:** clean (nothing uncommitted)

---

## Environment

| Setting | Value |
|---|---|
| Python | 3.12 (venv inside `backend/venv/`) |
| `EMBEDDING_PROVIDER` | `fastembed` |
| `LLM_PROVIDER` | `groq` |
| `GROQ_API_KEY` | **EMPTY — needs to be filled** |
| `QDRANT_URL` | `:memory:` (ephemeral) |
| Docker | Not available on this machine |

---

## Immediate Blocker Resolution

1. Go to https://console.groq.com/keys — create free API key (30 sec)
2. Open `backend/.env`
3. Set `GROQ_API_KEY=gsk_XXXXXXXXXXXXXXXXXXXX`
4. Server will hot-reload, or restart manually

---

## Next Steps After Groq Key

1. Re-upload a PDF (Qdrant in-memory is wiped on restart)
2. Test `POST /api/v1/query` → should return `{"answer": "...", "sources": [...], "metadata": {...}}`
3. Test `POST /api/v1/query/stream` → SSE token stream
4. Ingest all 5 arxiv PDFs
5. Build RAGAs evaluation pipeline:
   - `backend/app/evaluation/ragas_runner.py`
   - `data/eval/test_set.json` — 30 Q&A pairs
   - Run on 3 configs: Naive RAG, Dense-only, Hybrid+Rerank
   - `POST /api/v1/evaluation/run`
6. React frontend (Days 12–13)
7. Railway deploy + README (Day 14)

---

## Start Commands

```bash
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate

# Run server
uvicorn app.main:app --reload --port 8000

# In another terminal — test connection
curl http://localhost:8000/api/v1/health

# Upload + query test
curl -s -X POST http://localhost:8000/api/v1/documents/upload \
  -F "file=@../data/papers/2402.00161_RAG_for_LLMs.pdf"

curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is RAG?", "top_k": 3}' | python3 -m json.tool
```
