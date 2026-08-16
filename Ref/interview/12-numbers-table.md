# Lesson 12 — Numbers Table: Quick Reference

Memorize these cold. An interviewer will test whether you actually know your own project.

## Stack

| Layer | Technology |
|---|---|
| Web framework | FastAPI |
| Vector DB | Qdrant |
| Embeddings | fastembed — `BAAI/bge-small-en-v1.5`, 384 dims |
| Sparse retrieval | BM25 via `rank-bm25` |
| Fusion | Reciprocal Rank Fusion (RRF), k=60 |
| Reranking | Cohere `rerank-english-v3.0` |
| LLM | Groq Llama-3.3-70b-versatile |
| Document parsing | pypdf (PDF), python-docx (DOCX) |
| Evaluation | Custom LLM-as-judge + retrieval-only metrics |
| Tests | 39 pytest tests |

## Pipeline configs

| Config | Description |
|---|---|
| `vector_only` | Dense retrieval only |
| `vector_rerank` | Dense + Cohere rerank |
| `hybrid` | Dense + BM25 + RRF |
| `hybrid_rerank` | Dense + BM25 + RRF + Cohere rerank (default) |
| `long_context` | No retrieval; stuff chunks into prompt (control group) |

## Retrieval metrics (30 questions, 5 configs)

| Config | P@5 | R@5 | MRR | Avg relevant in top-5 |
|---|---:|---:|---:|---:|
| vector_only | 0.9733 | 0.0996 | 0.9778 | 4.87 |
| vector_rerank | **0.9933** | 0.1016 | **1.0000** | 4.97 |
| hybrid | 0.9533 | 0.1000 | 1.0000 | 4.77 |
| hybrid_rerank | 0.8533 | 0.0931 | 0.9167 | 4.27 |
| long_context | 0.2000 | 0.0099 | 0.3987 | 1.00 |

## Papers in the test set

| Filename | Topic |
|---|---|
| `2302.00093_LLM_Distractibility.pdf` | LLM distractibility by irrelevant context |
| `2305.18290_DPO.pdf` | Direct Preference Optimization |
| `2310.06825_Mistral_7B.pdf` | Mistral 7B |
| `2401.14295_Chains_Trees_Graphs_of_Thoughts.pdf` | Chain/tree/graph-of-thought reasoning |
| `2402.00161_QKD.pdf` | Device-independent quantum key distribution |

## Key constants and constraints

| Item | Value |
|---|---|
| RRF constant | k=60 |
| Cohere rate limit | 10 calls/minute (trial tier) |
| Cohere throttle in code | 6.1 seconds between calls |
| Default chunk size | 512 tokens |
| Chunk overlap | 50 tokens |
| Contextual summary length | max 15 words per section |
| Long context truncation | ~100,000 characters |
| Tests | 39 passed |

## Why numbers matter

- Precision is high because each paper's chunks are semantically distinct.
- Recall is low because each paper has 20–173 relevant chunks and you only take top-5.
- `vector_rerank` is the best config.
- `hybrid_rerank` (the default) underperforms — your headline insight.
- `long_context` is the control group proving retrieval matters.
