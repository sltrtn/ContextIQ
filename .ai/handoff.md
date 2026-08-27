# Handoff — Session End

> **Last updated:** 2026-08-20
> **Last action:** Android-backend endpoint alignment complete — Paper Analyzer uploads and queries the real backend; unsupported screens stubbed.

---

## Current Status

| Component | State |
|---|---|
| FastAPI server | ✅ runs (`uvicorn app.main:app --reload` in `backend/`) |
| Document upload | ✅ working — contextual chunking, 398 chunks from 5 papers |
| fastembed (BAAI/bge-small-en-v1.5, 384d) | ✅ installed, zero cost |
| Qdrant in-memory | ✅ works, **data lost on server restart** |
| Cohere Rerank | ✅ working |
| BM25 + RRF fusion | ✅ working |
| Groq LLM (openai/gpt-oss-120b) | ✅ working |
| Query rewriting/expansion | ✅ LLM generates 2-3 query variants |
| Context assembly | ✅ dedup, lost-in-the-middle ordering, source labels |
| Faithfulness post-check | ✅ LLM-as-judge claim verification |
| Ablation isolation | ✅ 5 pipeline configs via `config` parameter |
| RAGAs evaluation | ✅ LLM-as-judge runner + 30-item test set |
| Retrieval metrics | ✅ P@5, R@5, MRR across 30 questions × 5 configs |
| pytest test suite | ✅ 39 tests passing |
| Docker Compose | ✅ persistent Qdrant + backend |
| README | ✅ architecture, eval table, PDF mapping, getting started |
| Interview quick sheet | ✅ `Ref/interview/ContextIQ-Interview-Quick-Sheet.md` created |
| LinkedIn post draft | ✅ `Ref/portfolio/ContextIQ-LinkedIn-Post.md` created |
| Live portfolio screenshot | ⏳ Blocked in this environment (port/model-cache/network issue) |
| React frontend | ✅ `frontend/` is wired to upload/query APIs; full live validation pending reachable backend |
| Cross-platform design system | ✅ documented contract + Android/web tokens; Android migration is incremental |
| Railway deploy | ⏳ not started |

---

## What Was Built

### Core infrastructure
- `backend/app/main.py` — FastAPI app, lifespan, health endpoint
- `backend/app/core/config.py` — Pydantic settings, `.env` loading, `embedding_dim` property
- `backend/app/core/embeddings.py` — factory: `get_embed_model()` → fastembed or openai
- `backend/app/core/llm.py` — factory: `get_llm()` → groq or openai

### Ingestion pipeline
- `backend/app/ingestion/parser.py` — PDF (pypdf), DOCX (python-docx), TXT; `parse_document_pages()` for page-level metadata
- `backend/app/ingestion/chunker.py` — sentence_window, semantic, **contextual** (LLM-summarized section labels prepended to chunks)

### Retrieval pipeline
- `backend/app/retrieval/dense.py` — `get_qdrant_client()`, `ensure_collection()` (dynamic dim), singleton pattern
- `backend/app/retrieval/sparse.py` — `BM25Retriever` + **global BM25** singleton built at ingestion
- `backend/app/retrieval/fusion.py` — `reciprocal_rank_fusion()` (RRF_K=60)
- `backend/app/retrieval/reranker.py` — Cohere `rerank-english-v3.0` with fallback
- `backend/app/retrieval/query_transform.py` — **NEW** `expand_query()` generates 2-3 query variants via LLM
- `backend/app/retrieval/context_assembly.py` — **NEW** dedup, ordering, source labeling (`[1] filename.pdf (p.5): ...`)

### API routes
- `backend/app/api/routes/documents.py` — `POST /api/v1/documents/upload`, `GET /api/v1/documents/{id}/status`
- `backend/app/api/routes/query.py` — `POST /api/v1/query` with `config` + `expand` params, `POST /api/v1/query/stream` (SSE)
- `backend/app/api/routes/evaluation.py` — `POST /api/v1/evaluation/run`, `GET /api/v1/evaluation/configs`

### Evaluation
- `backend/app/evaluation/configs.py` — 5 pipeline configs: vector_only, vector_rerank, hybrid, hybrid_rerank, long_context
- `backend/app/evaluation/ragas_runner.py` — LLM-as-judge: faithfulness, answer_relevancy, context_precision, context_recall
- `backend/app/evaluation/faithfulness.py` — **NEW** post-generation faithfulness check with claim-level analysis

### Models
- `backend/app/models/query.py` — `QueryRequest` (config, expand), `QueryResponse` (faithfulness), `Source` (filename, page), `FaithfulnessCheck`

### Test set
- `data/eval/test_set.json` — 30 Q&A pairs across 5 papers

---

## Query API — Pipeline Configs

`POST /api/v1/query` supports these configs via the `config` parameter:

| Config | Pipeline |
|---|---|
| `vector_only` | Dense retrieval, top_k |
| `vector_rerank` | Dense + Cohere Rerank |
| `hybrid` | Dense + BM25 + RRF, no reranking |
| `hybrid_rerank` (default) | Dense + BM25 + RRF + Cohere Rerank |
| `long_context` | Stuff all chunks into context, no retrieval |

Set `"expand": true` to enable query rewriting (LLM generates 2-3 variants, retrieves for all, re-fuses).

---

## Repo State

- **Branch:** `main`
- **Remote:** `https://github.com/sltrtn/ContextIQ`
- **Working tree:** may have uncommitted changes

---

## Environment

| Setting | Value |
|---|---|
| Python | 3.12 (venv inside `backend/venv/`) |
| `EMBEDDING_PROVIDER` | `fastembed` |
| `LLM_PROVIDER` | `groq` |
| `GROQ_API_KEY` | Set in `backend/.env` (see credentials manager) |
| `QDRANT_URL` | `:memory:` (ephemeral) |
| Python binary | `/home/mad/StudioProjects/ContextIQ/backend/venv/bin/python` |

---

## Start Commands

```bash
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate

# Run server
uvicorn app.main:app --reload --port 8001

# Test
curl http://localhost:8001/api/v1/health

# Upload + query
curl -s -X POST http://localhost:8001/api/v1/documents/upload \
  -F "file=@../data/papers/2305.18290_DPO.pdf"

curl -s -X POST http://localhost:8001/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is QLoRA?", "top_k": 3, "config": "hybrid_rerank"}' | python3 -m json.tool

# Run evaluation
curl -s -X POST http://localhost:8001/api/v1/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"config": "vector_only", "max_questions": 5}' | python3 -m json.tool
```

---

## Next Steps

1. **Capture the LinkedIn live-query screenshot** — start from the exact command in `Ref/portfolio/ContextIQ-LinkedIn-Post.md`; crop to response-only and never expose `.env`/headers. If port 8001 remains stuck, identify and stop only the confirmed orphan before retrying.
2. **Validate / extend React frontend** — run the wired upload/query flow against a reachable backend, then add an observability dashboard.
3. **Android visual migration** — apply `ContextIQDesign` tokens to the next edited Compose screens, with emulator/device QA.
3. **Railway deploy** — Docker Compose + public URL
4. **Full LLM-judge evaluation** — requires paid Groq/OpenAI tier (free tier 100k tokens/day is insufficient for 150 calls)
5. **Contextual chunking improvement** — section detection is coarse, may need better regex
6. **Android rewire** — point Retrofit to deployed backend
