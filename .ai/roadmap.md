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
- ✅ `backend/.env` with working OpenAI + Cohere + Groq keys
- ✅ `test_connections.py` — all 3 services verified green
- ✅ `GET /api/v1/health` → 200 OK
- ✅ 5 arxiv PDFs in `data/papers/`

### Days 1–3 — Base RAG Pipeline ✅
- ✅ `ingestion/parser.py` — PDF/DOCX/TXT via pypdf, `parse_document_pages()` for page-level metadata
- ✅ `ingestion/chunker.py` — sentence_window, semantic, **contextual** (LLM-summarized section labels)
- ✅ `retrieval/dense.py` — Qdrant client + dynamic dim collection (singleton pattern)
- ✅ `retrieval/sparse.py` — BM25Retriever + **global BM25** singleton built at ingestion
- ✅ `retrieval/fusion.py` — Reciprocal Rank Fusion (RRF_K=60)
- ✅ `retrieval/reranker.py` — Cohere Rerank cross-encoder with fallback
- ✅ `api/routes/documents.py` — upload + status endpoints
- ✅ `api/routes/query.py` — hybrid query + SSE stream + config parameter
- ✅ `core/embeddings.py` — factory: fastembed (384d, local) / openai (1536d)
- ✅ `core/llm.py` — factory: groq (`openai/gpt-oss-120b`) / openai (GPT-4o-mini)

### Phase 0 — Bug Fixes + Global BM25 ✅
- ✅ Fixed `query.py` hardcoded OpenAI → `get_llm()`
- ✅ Fixed `dense.py` singleton pattern for in-memory Qdrant
- ✅ Fixed `documents.py` — wrapped vector store in `StorageContext.from_defaults()`
- ✅ Fixed `reranker.py` fallback to use input order
- ✅ Global BM25 built once at ingestion, reused across queries
- ✅ Metadata enrichment: page numbers, filenames on chunks and Source objects

### Phase 1 — Evaluation Pipeline ✅
- ✅ `data/eval/test_set.json` — 30 Q&A pairs (6 per paper)
- ✅ `evaluation/ragas_runner.py` — LLM-as-judge: faithfulness, relevancy, precision, recall
- ✅ `api/routes/evaluation.py` — `POST /api/v1/evaluation/run`, `GET /api/v1/evaluation/configs`
- ✅ 5 pipeline configs: vector_only, vector_rerank, hybrid, hybrid_rerank, long_context

### Phase 2 — Query Intelligence + Context Assembly ✅
- ✅ `retrieval/query_transform.py` — `expand_query()` generates 2-3 query variants via LLM
- ✅ `retrieval/context_assembly.py` — dedup, lost-in-the-middle ordering, source labels
- ✅ Contextual chunking: section detection, LLM summarization, prepended labels

### Phase 4 — Ablation Isolation ✅
- ✅ `QueryRequest` has `config` parameter (5 pipeline configs)
- ✅ `QueryRequest` has `expand` parameter (query rewriting toggle)
- ✅ Each config runs independently with isolated retrieval/generation

### Phase 5 — Faithfulness Post-Check ✅
- ✅ `evaluation/faithfulness.py` — claim extraction, context verification, score + unsupported claims
- ✅ Wired into `/query` response as `faithfulness` field

---

## Current Milestone

### ✅ Phase 6 — README, Tests, Retrieval Metrics, Docker
- ✅ Rename PDFs to match actual content
- ✅ pytest suite (39 tests)
- ✅ Retrieval metrics table (P@5, R@5, MRR)
- ✅ Docker Compose + persistent Qdrant
- ✅ README with architecture, eval table, PDF mapping

---

## Future Milestones

### ⏳ Persistent Qdrant + Docker Compose
- Docker Compose for Qdrant (persistent storage) + FastAPI
- Replace `:memory:` with volume-mounted Qdrant data
- `docker-compose up --build` for full local stack

### ⏳ Full Evaluation Run
- Run all 30 questions × 5 configs → comparison table
- Analyze failure cases (low faithfulness, low recall)
- Document findings in README

### ⏳ FastAPI Polish
- API key auth middleware
- Rate limiting
- Proper error handling + logging
- Pydantic v2 model improvements

### ⏳ React Frontend
- Chat UI consuming SSE stream
- Document upload widget
- Observability dashboard (latency, scores)
- RAGAs benchmark table

### ⏳ Deploy
- Dockerfile + docker-compose.yml
- Deploy to Railway
- Architecture diagram in README
- Loom demo recording

### ⏳ Android Rewire
- Point Retrofit base URL to deployed Railway backend
- SSE streaming in `ChatDetailScreen`
- Test all 14 screens against live backend
- APK distribution

---

## Technical Debt

- Android project lives at repo root (should move to `android/` subdirectory)
- `PaperAnalyzerScreen.kt` uses fully qualified package references
- Room DB uses `fallbackToDestructiveMigration()` — needs versioned migrations
- Sarvam compromised key still needs rotation at Sarvam dashboard
- Qdrant `:memory:` loses data on server restart — needs persistent Qdrant for production
- Contextual chunking section detection is coarse — regex needs tuning for diverse papers
- BM25 sparse results not carrying metadata (filename, page_number) — only dense results have it

---

## Planned Improvements

- Add user authentication (JWT, beyond static API key)
- Support more document formats (HTML, Markdown)
- Batch document upload
- Persistent query history with per-query scores
- Dark mode toggle on web frontend
- Android push notifications for ingestion completion
