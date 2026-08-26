# ContextIQ

> A production-grade RAG-powered document intelligence platform for research papers. Built to be **measured**, not just to work.

## Thesis

Most RAG projects are built to work. ContextIQ is built to be measured:
- **5 retrieval configurations** benchmarked against a **30-question test set**
- **Retrieval metrics** (P@5, R@5, MRR) computed across every config
- **Faithfulness post-check** on every query
- **Observability** built into the response

## Architecture

```
                    PDF/DOCX/TXT
                         │
                         ▼
              ┌─────────────────────┐
              │  Parse with pypdf   │
              │  page-level metadata│
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  Contextual chunking  │
              │  section detection +  │
              │  LLM summaries      │
              └─────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
   ┌─────────────┐                ┌─────────────┐
   │ Dense (384d)│                │ BM25 sparse │
   │   Qdrant    │                │  rank-bm25  │
   └─────────────┘                └─────────────┘
         │                               │
         └───────────────┬───────────────┘
                         ▼
              ┌─────────────────────┐
              │   RRF fusion (K=60) │
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  Cohere reranker    │
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  Context assembly     │
              │  dedup, ordering,     │
              │  source labels        │
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  LLM (Groq Llama)     │
              │  + faithfulness check │
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  Answer + sources +   │
              │  metadata + scores    │
              └─────────────────────┘
```

## Stack

| Layer | Technology |
|---|---|
| Backend | FastAPI, Pydantic, LlamaIndex |
| Embeddings | fastembed `BAAI/bge-small-en-v1.5` (384d, local) |
| Vector DB | Qdrant (`:memory:` for dev, Docker for persistence) |
| Sparse Retrieval | BM25 (rank-bm25) |
| Fusion | Reciprocal Rank Fusion (RRF, K=60) |
| Reranking | Cohere `rerank-english-v3.0` |
| LLM | Groq `openai/gpt-oss-120b` |
| Evaluation | Custom LLM-as-judge + retrieval metrics |
| Android | Kotlin, Jetpack Compose, Retrofit, Room |

## Quick Start

### Local development

```bash
cd backend
source venv/bin/activate
uvicorn app.main:app --reload --port 8001
```

Upload a paper and query:

```bash
# Health check
curl http://localhost:8001/api/v1/health

# Upload
curl -s -X POST http://localhost:8001/api/v1/documents/upload \
  -F "file=@data/papers/2305.18290_DPO.pdf"

# Query (full pipeline)
curl -s -X POST http://localhost:8001/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What does DPO stand for?", "top_k": 5, "config": "hybrid_rerank"}'

# Query with expansion
curl -s -X POST http://localhost:8001/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What does DPO stand for?", "expand": true}'
```

### Docker Compose

```bash
cd backend
docker compose up --build
```

This starts Qdrant (persistent volume) and the backend. Set your API keys in `.env` first.

## Pipeline Configs

`POST /api/v1/query` accepts a `config` parameter:

| Config | Description |
|---|---|
| `vector_only` | Dense retrieval, top_k |
| `vector_rerank` | Dense + Cohere rerank |
| `hybrid` | Dense + BM25 + RRF, no rerank |
| `hybrid_rerank` (default) | Dense + BM25 + RRF + Cohere rerank |
| `long_context` | Stuff all chunks into context, no retrieval |

## Evaluation

### Retrieval metrics (30 questions × 5 configs)

Measured offline, zero LLM cost. Relevance = retrieved chunk is from the same paper the question targets.

| Config | P@5 | R@5 | MRR | Avg relevant in top-5 |
|---|---:|---:|---:|---:|
| vector_only | 0.9733 | 0.0996 | 0.9778 | 4.87 |
| vector_rerank | **0.9933** | 0.1016 | **1.0000** | 4.97 |
| hybrid | 0.9533 | 0.1000 | 1.0000 | 4.77 |
| hybrid_rerank | 0.8533 | 0.0931 | 0.9167 | 4.27 |
| long_context | 0.2000 | 0.0099 | 0.3987 | 1.00 |

### What the numbers show

- **Dense + rerank wins on precision.** Cohere rerank on top of dense retrieval pushes the correct paper's chunks to the very top.
- **Hybrid without rerank is already strong.** BM25 + RRF fusion is competitive with dense-only.
- **Hybrid + rerank is counterintuitively weaker.** In 11/30 questions, Cohere rerank on the fused list pushed relevant chunks out of top-5. The fusion list is already high-quality; the reranker's cross-encoder signal sometimes disagrees with paper-level relevance.
- **Long context is a poor retriever.** As expected, stuffing all chunks gives only ~20% precision in top-5 and ~1% recall — it contains the answer somewhere, but the LLM has to find it.

### Run evaluation

```bash
# Full retrieval metrics
cd backend
python run_retrieval_metrics.py

# Full LLM-judge evaluation (requires paid Groq/OpenAI tier)
python run_eval.py

# API evaluation endpoint (subset)
curl -s -X POST http://localhost:8001/api/v1/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"config": "vector_only", "max_questions": 5}'
```

## Test Set

30 questions across 5 arXiv papers. Each question is annotated with the target paper, difficulty, and type.

| Paper | Filename | Actual content |
|---|---|---|
| 2302.00093 | `2302.00093_LLM_Distractibility.pdf` | Large Language Models Can Be Easily Distracted by Irrelevant Context |
| 2305.18290 | `2305.18290_DPO.pdf` | Direct Preference Optimization |
| 2310.06825 | `2310.06825_Mistral_7B.pdf` | Mistral 7B |
| 2401.14295 | `2401.14295_Chains_Trees_Graphs_of_Thoughts.pdf` | Demystifying Chains, Trees, and Graphs of Thoughts |
| 2402.00161 | `2402.00161_QKD.pdf` | Device-Independent Quantum Key Distribution beyond qubits |

## Test Suite

```bash
cd backend
pytest tests/ -v
```

39 unit tests covering parser, chunker (with mock LLM), BM25, RRF fusion, reranker fallback, context assembly, and query expansion fallback.

## Key Features

- **Contextual chunking**: Detects sections via regex, summarizes each with one LLM call, and prepends `[Section: name — summary]` to every chunk.
- **Query expansion**: Generates 2-3 query variants, retrieves for each, and re-fuses with RRF.
- **Context assembly**: Deduplicates near-duplicate chunks, orders chunks for lost-in-the-middle attention, and labels sources as `[1] filename.pdf (p.5): ...`.
- **Faithfulness post-check**: After generation, an LLM-as-judge extracts claims and verifies each against the retrieved contexts. Response includes score, supported/total claims, and unsupported claim list.

## Project Structure

```
backend/
  app/
    api/routes/        # documents, query, evaluation
    core/              # config, embeddings, llm
    ingestion/         # parser, chunker
    evaluation/        # configs, ragas_runner, retrieval_metrics, faithfulness
    models/            # Pydantic schemas
    retrieval/         # dense, sparse, fusion, reranker, query_transform, context_assembly
  tests/               # pytest suite
  run_retrieval_metrics.py
  run_eval.py
  docker-compose.yml
  Dockerfile
```

## Environment

Copy `.env.template` to `.env` and set:

```
GROQ_API_KEY=your_key_here
COHERE_API_KEY=...
OPENAI_API_KEY=sk-...     # optional fallback
QDRANT_URL=:memory:       # use http://qdrant:6333 with Docker
```

## Notes

- The Groq free tier has a 100k tokens/day limit. The full LLM-judge evaluation (`run_eval.py`) may exceed this; run it with a paid tier or in batches.
- The Cohere trial key is limited to 10 calls/minute. The reranker automatically throttles to stay under this limit.
- Data in `:memory:` Qdrant is lost on server restart. Use Docker Compose for persistence.

## License

MIT
