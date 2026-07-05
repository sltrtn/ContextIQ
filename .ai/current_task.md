# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Day 1–3 — Base RAG Pipeline: ingestion → embedding → query end-to-end.**

---

## Status

🔄 In Progress — End-to-end pipeline running, first test upload in progress.

---

## Completed

- [x] Python environment: venv set up, all dependencies installed
- [x] `backend/` directory structure created with all subpackages
- [x] `backend/app/core/config.py` — Pydantic settings with .env loading
- [x] `backend/app/main.py` — FastAPI app with `/api/v1/health`
- [x] `backend/test_connections.py` — verified OpenAI + Qdrant + Cohere ✅
- [x] `backend/.env` filled with working API keys ✅
- [x] `data/papers/` — 5 arxiv PDFs downloaded ✅
- [x] `backend/app/ingestion/parser.py` — PDF/DOCX/TXT parser (pypdf)
- [x] `backend/app/ingestion/chunker.py` — sentence_window + semantic chunking
- [x] `backend/app/ingestion/tasks.py` — Celery ingest task scaffold
- [x] `backend/app/retrieval/dense.py` — Qdrant client + collection setup
- [x] `backend/app/retrieval/sparse.py` — BM25Retriever (rank_bm25)
- [x] `backend/app/retrieval/fusion.py` — Reciprocal Rank Fusion
- [x] `backend/app/retrieval/reranker.py` — Cohere Rerank wrapper
- [x] `backend/app/api/routes/documents.py` — POST /upload, GET /{id}/status
- [x] `backend/app/api/routes/query.py` — POST /query + POST /query/stream (SSE)
- [x] `backend/app/models/document.py` + `query.py` — Pydantic schemas
- [x] FastAPI health check `/api/v1/health` → 200 OK ✅
- [x] Day 0 connection test: All connections OK ✅
- [x] First PDF upload + embedding test in progress

## Next Steps

- [ ] Verify first upload returns `{"status":"completed", ...}` via API
- [ ] Test `/api/v1/query` with a question about the uploaded paper
- [ ] Test `/api/v1/query/stream` SSE endpoint
- [ ] Download + ingest all 5 arxiv PDFs
- [ ] Add evaluation route (`/api/v1/evaluation/run`)
- [ ] Build RAGAs 30-question test set
- [ ] Write `backend/app/evaluation/ragas_runner.py`

---

## Next Immediate Step

```bash
# With FastAPI server running on port 8000:
curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is retrieval augmented generation?"}' | python3 -m json.tool
```
