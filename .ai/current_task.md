# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Unblock the LLM — add Groq API key, then move to RAGAs evaluation pipeline.**

---

## Status

❌ Blocked — `GROQ_API_KEY` is empty in `backend/.env`. LLM calls fail; embedding + retrieval fully work.

---

## What Works Right Now

- [x] `POST /api/v1/documents/upload` → parse → 76 chunks → fastembed → Qdrant ✅
- [x] Dense retrieval (Qdrant + fastembed query) ✅
- [x] BM25 sparse retrieval ✅
- [x] RRF fusion ✅
- [x] Cohere Rerank ✅
- [ ] LLM answer generation — **blocked on Groq key**
- [ ] Full `/api/v1/query` response with answer + sources
- [ ] `/api/v1/query/stream` SSE streaming (also blocked on LLM)

---

## Blocker

```
GROQ_API_KEY=        ← needs a value
```

File: `backend/.env`  
Get free key: https://console.groq.com/keys (30 sec, no card)

---

## Next Steps (in order)

1. **[ ] Paste Groq key** into `backend/.env` at `GROQ_API_KEY=gsk_...`
2. **[ ] Restart server** and re-upload a PDF (in-memory Qdrant resets on restart)
3. **[ ] Verify query returns answer + sources** — marks Days 1–3 fully done
4. **[ ] Ingest all 5 arxiv PDFs** into Qdrant
5. **[ ] Build `backend/app/evaluation/ragas_runner.py`** — RAGAs pipeline
6. **[ ] Create 30-question test set** (`data/eval/test_set.json`)
7. **[ ] Run RAGAs across 3 configs** — Naive, Dense-only, Hybrid+Rerank
8. **[ ] Add `POST /api/v1/evaluation/run`** endpoint
9. **[ ] React frontend** (Days 12–13)
10. **[ ] Deploy to Railway** (Day 14)

---

## Immediate Commands (once Groq key is set)

```bash
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate

# Start server
uvicorn app.main:app --reload

# Upload test paper
curl -s -X POST http://localhost:8000/api/v1/documents/upload \
  -F "file=@../data/papers/2402.00161_RAG_for_LLMs.pdf"

# Query
curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is Retrieval Augmented Generation?", "top_k": 3}' \
  | python3 -m json.tool
```
