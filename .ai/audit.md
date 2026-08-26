# ContextIQ — Complete Audit (2026-08-20)

> What's implemented, what's broken, and what remains for production / portfolio readiness.

---

## Status Summary

| Area | State |
|---|---|
| FastAPI backend | ✅ Functional |
| RAG pipeline (5 configs) | ✅ Working |
| Tests | ✅ 39 passing |
| Docker Compose | ✅ Files present |
| React frontend | ⚠️ Built but **uncommitted** |
| Android app | ❌ **Endpoint mismatch with backend** |
| Docs / README | ⚠️ Outdated model name (Llama 3.3 70B retired) |
| Deployment | ❌ Not deployed |

---

## 1. Implemented & Working

### Backend (FastAPI)
- `POST /api/v1/documents/upload` — PDF/DOCX/TXT ingestion
- `POST /api/v1/query` — 5 pipeline configs + faithfulness score
- `POST /api/v1/query/stream` — SSE streaming
- `GET/POST /api/v1/evaluation/*` — configs + custom LLM-as-judge
- `GET /api/v1/health` — returns active model name

### RAG Pipeline
- **Parsing:** page-level PDF extraction with `pypdf`
- **Chunking:** contextual chunking with LLM section summaries
- **Dense:** Qdrant + fastembed `BAAI/bge-small-en-v1.5` (384d)
- **Sparse:** global BM25 singleton built at ingestion
- **Fusion:** Reciprocal Rank Fusion (`K=60`)
- **Reranking:** Cohere `rerank-english-v3.0` with fallback + rate limiter
- **Generation:** Groq-hosted model (currently `openai/gpt-oss-120b`)
- **Faithfulness:** claim-level LLM-as-judge on every live query

### Evaluation
- 30-question test set across 5 papers
- Retrieval-only metrics: P@5, R@5, MRR
- LLM-as-judge metrics: faithfulness, answer relevancy, context precision, context recall
- 39 pytest tests passing

### DevOps
- `Dockerfile` + `docker-compose.yml` present
- `.env` template for local dev

---

## 2. Critical Issues (Fix Before Demo/Deploy)

### 2.1 Android ↔ Backend Endpoint Mismatch

**Severity:** Critical

The Android app calls endpoints that **do not exist** in the backend:

| Android calls (`ContextIQApi.kt`) | Backend provides |
|---|---|
| `POST analyze/image` | ❌ Does not exist |
| `POST analyze/literature-review` | ❌ Does not exist |
| `POST analyze/abstract` | ❌ Does not exist |
| `POST analyze/claim-verify` | ❌ Does not exist |
| `POST analyze/journal-match` | ❌ Does not exist |
| `POST analyze/paper-review` | ❌ Does not exist |
| `POST analyze/latex` | ❌ Does not exist |
| `POST analyze/rebuttal` | ❌ Does not exist |
| `POST chat/stream` | ❌ Does not exist (backend has `/query/stream`) |
| `POST tools/citation` | ❌ Does not exist |
| `POST tools/related-papers` | ❌ Does not exist |
| `POST tools/open-access` | ❌ Does not exist |

Backend only has:
- `POST /api/v1/documents/upload`
- `POST /api/v1/query`
- `POST /api/v1/query/stream`
- `POST /api/v1/evaluation/run`
- `GET /api/v1/evaluation/configs`

**Action:** Either rewrite Android screens to use backend endpoints, or add backend endpoints for the Android features.

---

### 2.2 Groq Model Retired

**Severity:** Critical

`llama-3.3-70b-versatile` is no longer available on Groq. We patched `backend/app/core/config.py` to use `openai/gpt-oss-120b`, but:

- `README.md` still says "Groq `llama-3.3-70b-versatile`"
- `.ai/project.md` still says "Groq Llama 3.3 70B Versatile"
- `.ai/roadmap.md` still says "Groq Llama 3.3 70B"
- Stack table, architecture diagram, and handoff docs are all stale

**Action:** Bulk-update all docs to reflect the actual model.

---

### 2.3 Qdrant In-Memory Loses Data on Restart

**Severity:** High

`QDRANT_URL=:memory:` means all indexed documents are lost when the server restarts. Docker Compose exists but runs in `backend/` and isn't the default dev workflow.

**Action:** Switch default dev to Docker Compose with persistent Qdrant, or at least document that re-upload is required after restart.

---

### 2.4 Cohere Trial Rate Limit Makes Evaluation Painfully Slow

**Severity:** High

Cohere trial key is limited to 10 calls/minute. The reranker has a `_rate_limit()` guard (6.1s sleep between calls), so:
- `vector_rerank` takes ~178s for 30 questions
- `hybrid_rerank` takes ~179s
- Running full `run_eval.py` or `run_retrieval_metrics.py` is impractical

**Action:** Upgrade to paid Cohere tier, or add a flag to skip reranking during rapid iteration.

---

### 2.5 Faithfulness Judge Truncates Context

**Severity:** Medium-High

`backend/app/evaluation/faithfulness.py:17` truncates each context to first 500 chars:
```python
context_block = "\n\n---\n\n".join(f"[Context {i+1}]\n{c[:500]}" ...)
```

This artificially lowers faithfulness scores because the judge sees less context than the generator saw.

**Action:** Increase to ~2000 chars or feed full context (watch token budget).

---

### 2.6 Frontend Built but Uncommitted

**Severity:** Medium

A React + Vite frontend exists in `frontend/` with a polished single-file UI, but:
- Directory is untracked (`?? frontend/`)
- `.gitignore` was modified to ignore it
- It is not part of the repo

**Action:** Decide whether to commit it or delete it. If keeping, add proper `README` instructions and commit.

---

### 2.7 Port 8000 Blocked by Orphaned Judge Process

**Severity:** Medium

Port 8000 has an orphaned socket from a previous judge project (no process, but socket occupied). Backend currently runs on 8001, but:
- README examples use port 8000
- Docker Compose exposes 8000
- Android `build.gradle.kts` points to `10.0.2.2:8000`

**Action:** Either free port 8000 or standardize the entire project on 8001.

---

### 2.8 Many Uncommitted Changes in Working Tree

**Severity:** Medium

`git status` shows modified files:
- `.ai/current_task.md`
- `.ai/design-system.md`
- `.ai/handoff.md`
- `.ai/progress.md`
- `.gitignore`
- `app/src/main/java/com/contextiq/app/ui/components/ExpressiveUtils.kt`
- `app/src/main/java/com/contextiq/app/ui/theme/Theme.kt`
- `backend/app/core/config.py`
- Untracked: `Ref/portfolio/`, `ContextIQDesign.kt`, `frontend/`

**Action:** Review, commit, or revert these changes so the repo is clean.

---

## 3. What Remains for Implementation

### 3.1 Must-Have for Portfolio

1. **Fix Android-backend integration**
   - Align Android endpoints with backend, OR
   - Implement backend endpoints that Android expects
2. **Update all documentation**
   - Replace `llama-3.3-70b-versatile` with actual model name everywhere
   - Fix README test count (says 33, actual 39)
   - Update port references to current reality
3. **Persistent Qdrant as default**
   - Make Docker Compose the recommended dev path
   - Verify upload → restart → query still works
4. **Commit or remove frontend**
5. **Clean working tree**

### 3.2 Should-Have for Production

1. **API authentication**
   - Currently no auth middleware
2. **Rate limiting on `/query` and `/upload`**
3. **Proper logging / observability**
   - No structured logs beyond print statements
4. **Faithfulness context window fix**
5. **Pydantic v2 model cleanup**
6. **Dependency pinning**
   - `frontend/package.json` uses `"latest"` for all deps (unstable)
   - `requirements.txt` should be reviewed for pins
7. **SSE streaming frontend**
   - Current frontend only uses `/query`, not `/query/stream`
8. **Evaluation dashboard**
   - Display retrieval metrics and LLM-judge scores in UI

### 3.3 Nice-to-Have

1. **Move Android app to `android/` subdirectory**
2. **Dark mode toggle**
3. **Batch document upload**
4. **Persistent chat history**
5. **Push notifications on Android**
6. **Support HTML/Markdown documents**
7. **Loom demo recording**
8. **Railway deploy with public URL**

---

## 4. Known Runtime Issues

| Issue | Symptom | Workaround |
|---|---|---|
| Groq model retired | `404 model_not_found` | Use `openai/gpt-oss-120b` |
| Cohere rate limit | Rerank configs take ~3 min | Wait, or use non-rerank configs |
| `:memory:` Qdrant | Data lost on restart | Re-upload after restart |
| Port 8000 occupied | `Address already in use` | Use port 8001 |
| Faithfulness truncation | Low faithfulness scores | Increase `c[:500]` limit |
| Android endpoints missing | All Android API calls fail | Needs endpoint rewrite |

---

## 5. Security Audit

| Item | Status | Notes |
|---|---|---|
| API keys in `.env` | ✅ Correct | Not committed |
| Hardcoded keys in Android | ✅ Removed | All AI through backend |
| Sarvam compromised key | ⚠️ Stale | Removed from code, but should rotate at Sarvam dashboard if account still used |
| API auth | ❌ Missing | Anyone with network access can call backend |
| Input validation | ⚠️ Basic | File extension check exists; no size limits or content scanning |
| CORS | ⚠️ Not checked | May need config for deployed frontend |

---

## 6. Recommended Priority Order

### This Week (Before Interview)
1. Update README + all `.ai/` docs to correct model name
2. Decide: commit frontend or delete it
3. Clean `git status` — commit/revert pending changes
4. Fix port inconsistency (8000 vs 8001)
5. Increase faithfulness context window for defensible scores

### Next Week (Before Deploy)
6. Fix Android-backend endpoint mismatch
7. Switch default dev to Docker Compose persistent Qdrant
8. Add basic API auth / rate limiting
9. Run full LLM-judge evaluation (paid tier or batch overnight)

### Later
10. Railway deploy
11. Frontend SSE + eval dashboard
12. Android app polish + APK distribution

---

## 7. Quick Health Check Commands

```bash
# Tests
cd backend && pytest tests/ -q

# Server (use 8001 until 8000 is freed)
cd backend && source venv/bin/activate && uvicorn app.main:app --port 8001

# Upload + query
curl -s -X POST http://localhost:8001/api/v1/documents/upload \
  -F "file=@data/papers/2305.18290_DPO.pdf"

curl -s -X POST http://localhost:8001/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question":"What is DPO?","config":"vector_only","top_k":3}'
```
