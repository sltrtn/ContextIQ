# ContextIQ — Master Document

---

## What It Is

A production-grade RAG-powered document intelligence platform targeting the research paper domain. Users upload documents, ask questions, and get accurate sourced answers — with the system being **evaluated, observable, and measurable**, not just functional.

**The core thesis:** Most RAG projects are built to work. ContextIQ is built to be measured. Nobody does the evaluation. You run RAGAs across three retrieval configurations, produce a comparison table with real numbers, and show live observability metrics per query. That's the difference between a portfolio project and an engineering contribution.

**Portfolio role:** AI credibility card — covers ML, Backend, and Android roles from a single project.

**Origin story:** Built Scholium (Kotlin Android research assistant, 12 screens, Gemini vision + Sarvam AI). Identified its core failure — no semantic retrieval, raw image-to-Gemini with no grounding, not measurable, not scalable. ContextIQ is the production backend that fixes every one of those problems. Two projects, one arc.

---

## What Makes It Different From Every Other RAG Project

1. **The benchmark table** — three configs, four metrics, real numbers. Nobody does this.
2. **The evolution story** — Scholium → ContextIQ is a judgment call, not a tutorial follow. Shows you identify problems and engineer solutions.
3. **Cross-platform** — one backend, React web + Android client. Shows systems thinking.
4. **Observability** — live per-query metrics panel. Shows production mindset.
5. **Research paper domain** — specific, credible, real use case with real data.

---

## Architecture — The Big Picture

```
User uploads PDF/DOCX/TXT
          ↓
FastAPI upload endpoint
          ↓
Celery worker (async, non-blocking)
          ↓
unstructured → parse document
          ↓
Chunking (semantic / sentence-window)
          ↓
OpenAI embeddings → Qdrant (cloud)
          ↓
[Document ready]

User asks question
          ↓
FastAPI query endpoint
          ↓
BM25 sparse retrieval ──┐
                        ├→ RRF fusion → Cohere Rerank → LlamaIndex QE → LLM
Dense retrieval ────────┘
          ↓
Streaming SSE response + sources + observability metadata
          ↓
React Web UI  /  Android Kotlin Client
```

---

## Stack — Every Layer

| Layer | Technology | Why |
|---|---|---|
| Retrieval Framework | LlamaIndex | Query orchestration, index management |
| Vector Store | Qdrant Cloud | Production-grade, free tier available |
| Embeddings | OpenAI `text-embedding-3-small` | Quality + cost balance |
| Sparse Retrieval | BM25 (rank-bm25) | Keyword precision, complements dense |
| Fusion | Reciprocal Rank Fusion (RRF) | Combines sparse + dense without score normalization issues |
| Reranking | Cohere Rerank (cross-encoder) | Re-scores top-k results by semantic relevance |
| LLM | OpenAI GPT-4o-mini | Cost-effective, strong instruction following |
| Backend | FastAPI (async) | Performance, modern Python, easy SSE |
| Task Queue | Celery + Redis | Async document ingestion, non-blocking API |
| Document Parsing | `unstructured` | Handles PDF, DOCX, TXT edge cases |
| Evaluation | RAGAs | Faithfulness, answer relevancy, context precision, context recall |
| Web Frontend | React + Recharts | Chat UI + observability dashboard |
| Android Frontend | Kotlin + Jetpack Compose + Retrofit | Scholium 2.0, same patterns already known |
| Local DB (Android) | Room DB | Offline chat history, session management |
| Infra | Docker Compose | FastAPI + Redis + Celery containerized |
| Deployment | Railway or Render | FastAPI backend hosted, public URL |
| Vector DB (hosted) | Qdrant Cloud free tier | No self-hosting needed |

---

## Project Structure

```
contextiq/
├── backend/
│   ├── app/
│   │   ├── api/
│   │   │   ├── routes/
│   │   │   │   ├── documents.py      # upload, status, list
│   │   │   │   ├── query.py          # ask, streaming
│   │   │   │   └── evaluation.py     # RAGAs trigger, results
│   │   │   └── dependencies.py       # auth, rate limiting
│   │   ├── core/
│   │   │   ├── config.py             # .env loading, settings
│   │   │   └── security.py           # API key middleware
│   │   ├── ingestion/
│   │   │   ├── tasks.py              # Celery tasks
│   │   │   ├── parser.py             # unstructured wrappers
│   │   │   └── chunker.py            # chunking strategies
│   │   ├── retrieval/
│   │   │   ├── dense.py              # Qdrant dense retriever
│   │   │   ├── sparse.py             # BM25 retriever
│   │   │   ├── fusion.py             # RRF implementation
│   │   │   └── reranker.py           # Cohere Rerank wrapper
│   │   ├── evaluation/
│   │   │   ├── ragas_pipeline.py     # RAGAs runner
│   │   │   ├── test_set.py           # 30-question test set
│   │   │   └── results/              # stored benchmark JSONs
│   │   ├── models/
│   │   │   ├── document.py           # Pydantic models
│   │   │   └── query.py
│   │   └── main.py
│   ├── data/
│   │   └── papers/                   # local arxiv PDFs for testing
│   ├── .env
│   ├── requirements.txt
│   ├── Dockerfile
│   └── docker-compose.yml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── ChatWindow.jsx
│   │   │   ├── DocumentUpload.jsx
│   │   │   ├── ObservabilityPanel.jsx
│   │   │   └── MetricsTable.jsx
│   │   ├── pages/
│   │   │   ├── Home.jsx
│   │   │   └── Dashboard.jsx
│   │   └── App.jsx
│   └── package.json
├── android/                          # Scholium 2.0
│   └── app/src/main/java/
│       └── com/example/contextiq/
│           ├── data/
│           │   ├── local/            # Room DB (from Scholium)
│           │   ├── remote/           # Retrofit API client (new)
│           │   └── repository/
│           ├── ui/
│           │   ├── screens/          # Scholium screens, rewired
│           │   └── components/
│           └── utils/
│               ├── PdfUtils.kt       # reused from Scholium
│               └── OCRHelper.kt      # reused from Scholium
└── README.md
```

---

## API Contract — Backend Endpoints

**Document Management**
```
POST   /api/v1/documents/upload       # upload file, triggers Celery task
GET    /api/v1/documents/{id}/status  # ingestion status (pending/ready/failed)
GET    /api/v1/documents/             # list all ingested documents
DELETE /api/v1/documents/{id}         # remove document + vectors
```

**Query**
```
POST   /api/v1/query                  # ask question, returns answer + sources + metadata
POST   /api/v1/query/stream           # same but SSE streaming
```

**Evaluation**
```
POST   /api/v1/evaluation/run         # trigger RAGAs on test set
GET    /api/v1/evaluation/results     # fetch latest benchmark results
```

**Health**
```
GET    /api/v1/health                 # API status, Qdrant connection, Redis status
```

---

## Retrieval Pipeline — Detailed

**Step 1 — Ingestion**
- `unstructured` parses PDF/DOCX/TXT
- Two chunking strategies tested: semantic chunking and sentence-window (size 512, overlap 50)
- OpenAI `text-embedding-3-small` generates vectors
- Vectors + metadata stored in Qdrant collection
- Raw text stored alongside for BM25 index

**Step 2 — Retrieval (on query)**
- BM25 runs on stored raw chunks → returns top-20 with scores
- Qdrant dense search → returns top-20 with scores
- RRF fusion merges both lists → produces unified top-20
- Cohere Rerank re-scores unified list → final top-5 passed to LLM

**Step 3 — Generation**
- LlamaIndex query engine takes top-5 chunks + user question
- GPT-4o-mini generates answer with source attribution
- Response streamed via SSE with metadata: latency, retrieval scores, reranking deltas, token usage

---

## RAGAs Evaluation Pipeline — The Differentiator

**Test set:** 30 questions manually written on 5–10 real arxiv papers

**Three configurations benchmarked:**

| Configuration | Description |
|---|---|
| Naive RAG | Direct dense retrieval, no reranking, GPT-4o-mini |
| Dense Only | Qdrant dense + Cohere Rerank, no BM25 |
| Hybrid + Rerank | BM25 + Dense + RRF + Cohere Rerank (full pipeline) |

**Metrics per configuration:**
- **Faithfulness** — does the answer stick to the retrieved context?
- **Answer Relevancy** — is the answer relevant to the question?
- **Context Precision** — are the retrieved chunks actually useful?
- **Context Recall** — did retrieval capture everything needed?

**Target output:** A comparison table with real numbers. This goes in the README, the resume bullet, and you quote it in every interview.

| Configuration | Faithfulness | Answer Relevancy | Context Precision | Context Recall |
|---|---|---|---|---|
| Naive RAG | TBD | TBD | TBD | TBD |
| Dense Only | TBD | TBD | TBD | TBD |
| Hybrid + Rerank | TBD | TBD | TBD | TBD |

---

## Observability Panel (React Web)

**Live metrics per query:**
- End-to-end latency (ms)
- BM25 top-3 scores
- Dense retrieval top-3 scores
- Post-RRF scores
- Post-Cohere-Rerank scores
- Token usage (prompt + completion)
- RAGAs scores for the query (async, shown after generation)

**Dashboard tab:**
- RAGAs benchmark table (all three configs)
- Aggregate latency over last N queries
- Document count, collection size

---

## Scholium → ContextIQ Migration Map

| Scholium Screen | What It Did | ContextIQ Equivalent |
|---|---|---|
| Paper Analyzer | PDF → Gemini vision → answer | `POST /query` with document context |
| Literature Reviewer | 3 PDFs → Sarvam chat → review | Multi-document RAG query |
| Claim Verifier | Statement → Gemini → verify | RAG query with faithfulness check |
| Abstract Summarizer | PDF → Gemini → summary | RAG summarization query |
| Related Papers | Semantic Scholar API | Qdrant similarity search |
| Citation Generator | Crossref API call | Metadata from ingested documents |
| Paper Reviewer | Gemini prompt | RAG-grounded review generation |
| LaTeX Generator | Gemini prompt | RAG-grounded LaTeX output |
| Journal Matcher | Gemini prompt | Similarity search on journal corpus |
| Review Rebuttal | Gemini prompt | RAG-grounded rebuttal generation |

**What's reused from Scholium as-is:**
- `PdfUtils.kt` — PDF rendering and page navigation
- `OCRHelper.kt` — ML Kit OCR
- Room DB schema (ChatSession, ChatMessage entities)
- All Jetpack Compose UI patterns and theming
- Navigation structure

**What's replaced:**
- `SarvamApiService.kt` → deleted, replaced with Retrofit client hitting ContextIQ backend
- All direct Gemini API calls in `PaperAnalyzerScreen.kt` → replaced with backend calls
- Hardcoded API keys → gone entirely from Android codebase

---

## Security Fixes (from Scholium)

- Hardcoded Sarvam API key in `SarvamApiService.kt` → **rotate immediately** on Sarvam dashboard
- Gemini key in `PaperAnalyzerScreen.kt` → move to `local.properties` → `BuildConfig`
- All Android API calls → backend only, no AI keys ever touch the Android app again
- Backend keys → `.env` only, never committed, always in `.gitignore`

---

## Build Timeline — 19 Days

| Days | Focus | Deliverable |
|---|---|---|
| 0 | Setup — accounts, env, project scaffold, pip install, test connections, 5 arxiv PDFs downloaded | `test_connections.py` prints success for OpenAI + Qdrant + Cohere |
| 1–3 | Base RAG pipeline — LlamaIndex + Qdrant + OpenAI, single document ingestion, basic query end-to-end | Can ask a question about an uploaded paper and get an answer |
| 4–5 | Hybrid search — BM25 retriever + RRF fusion | Hybrid retrieval working, scores visible in logs |
| 6–7 | Cohere Rerank + Celery async ingestion + unstructured parsing | Full pipeline end-to-end, ingestion non-blocking |
| 8–9 | RAGAs evaluation — 30 question test set, three config benchmark table | Comparison table with real numbers |
| 10–11 | FastAPI polish — auth middleware, rate limiting, SSE streaming, error handling, Pydantic models | Production-ready API |
| 12–13 | React web frontend — chat UI + observability dashboard + metrics panel | Recruiter-demo-ready web app |
| 14 | Docker Compose + deploy to Railway + README with architecture diagram + Loom demo | Live URL in README |
| 15–19 | Android client — Kotlin/Compose, Retrofit, Room DB, all Scholium screens rewired to backend | Scholium 2.0 APK |

---

## Day 0 Checklist

**Accounts + API Keys**
- OpenAI API key — platform.openai.com
- Cohere API key — cohere.com (free tier)
- Qdrant Cloud account — cloud.qdrant.io (free tier, grab URL + API key)
- Railway or Render account — for Day 14

**Python Environment**
```bash
python -m venv venv
source venv/bin/activate
pip install fastapi uvicorn llama-index llama-index-vector-stores-qdrant \
  qdrant-client openai cohere rank-bm25 ragas celery redis \
  unstructured python-dotenv httpx sse-starlette
pip freeze > requirements.txt
```

**Connection Test — `test_connections.py`**
```python
from openai import OpenAI
from qdrant_client import QdrantClient
import cohere, os
from dotenv import load_dotenv
load_dotenv()

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
print("OpenAI:", client.models.list().data[0].id)

q = QdrantClient(url=os.getenv("QDRANT_URL"), api_key=os.getenv("QDRANT_API_KEY"))
print("Qdrant:", q.get_collections())

co = cohere.Client(os.getenv("COHERE_API_KEY"))
print("Cohere:", co.tokenize(text="test", model="command").tokens[:2])
```

**docker-compose.yml**
```yaml
version: '3.8'
services:
  api:
    build: .
    ports:
      - "8000:8000"
    env_file: .env
    depends_on:
      - redis

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  celery:
    build: .
    command: celery -A app.ingestion.tasks worker --loglevel=info
    env_file: .env
    depends_on:
      - redis
```

---

## Resume Bullets (fill in X after running RAGAs)

**ML/AI roles:**
> "Improved retrieval accuracy by X% over naive RAG baseline using hybrid BM25 + dense search with cross-encoder reranking, evaluated across faithfulness, answer relevancy, and context precision via RAGAs on a 30-question arxiv test set"

**Backend roles:**
> "Designed async FastAPI backend with Celery ingestion pipeline, SSE streaming, Qdrant vector store, and API key auth — containerized via Docker Compose and deployed on Railway"

**Android roles:**
> "Built Kotlin/Compose Android client consuming REST and SSE streaming APIs with offline chat history via Room DB — evolved from Scholium research assistant, eliminating all on-device AI calls in favor of a production RAG backend"

---

## README Headline (use this verbatim)

> *Most RAG implementations are evaluated informally — "it gave a good answer." ContextIQ treats retrieval quality as an engineering metric. Three retrieval configurations are benchmarked using RAGAs across faithfulness, answer relevancy, and context precision. Hybrid BM25 + dense retrieval with cross-encoder reranking outperforms naive RAG by X% on a 30-question test set built from real arxiv papers.*

---

*Last updated: Day 0*
