# Current Task

> Always represents exactly what is currently being worked on.

---

## Objective

**Phase 6 — README overhaul + final polish. All core features are implemented and tested.**

---

## Status

✅ All core features working. Backend runs, queries return answers with faithfulness scores.

---

## What Works Right Now

- [x] `POST /api/v1/documents/upload` → contextual chunking → fastembed → Qdrant ✅
- [x] Dense retrieval (Qdrant + fastembed query) ✅
- [x] Global BM25 sparse retrieval (built at ingestion) ✅
- [x] RRF fusion ✅
- [x] Cohere Rerank with fallback ✅
- [x] LLM answer generation (Groq Llama 3.3 70B) ✅
- [x] Query expansion (LLM generates 2-3 variants) ✅
- [x] Context assembly (dedup, ordering, source labels) ✅
- [x] Faithfulness post-check (claim extraction + verification) ✅
- [x] 5 pipeline configs (vector_only, vector_rerank, hybrid, hybrid_rerank, long_context) ✅
- [x] LLM-as-judge evaluation runner ✅
- [x] 30-question test set ✅
- [ ] README overhaul — **next**
- [ ] Full evaluation run (30 questions × 5 configs)
- [ ] Persistent Qdrant (Docker Compose)
- [ ] React frontend
- [ ] Deploy to Railway

---

## Next Steps (in order)

1. **[ ] Write README** — architecture diagram, results table, getting started
2. **[ ] Run full evaluation** — 30 questions × 5 configs → comparison table
3. **[ ] Persistent Qdrant** — Docker Compose with volume mount
4. **[ ] React frontend** — chat UI + upload + observability dashboard
5. **[ ] Deploy to Railway** — Docker Compose + public URL
6. **[ ] Android rewire** — point Retrofit to deployed backend

---

## Start Commands

```bash
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate

# Start server
uvicorn app.main:app --reload --port 8000

# Upload test paper
curl -s -X POST http://localhost:8000/api/v1/documents/upload \
  -F "file=@../data/papers/2305.18290_QLoRA.pdf"

# Query (full pipeline)
curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is QLoRA?", "top_k": 5, "config": "hybrid_rerank"}' \
  | python3 -m json.tool

# Query with expansion
curl -s -X POST http://localhost:8000/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is QLoRA?", "expand": true}' \
  | python3 -m json.tool

# Run evaluation (5 questions)
curl -s -X POST http://localhost:8000/api/v1/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"config": "vector_only", "max_questions": 5}' \
  | python3 -m json.tool
```
