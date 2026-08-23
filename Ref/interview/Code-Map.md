# ContextIQ — Code Map

> Every file. Every key function. One line each.
> This is how you point at any code block and explain what it does.

---

## Data Flow (follow this order)

```
Upload PDF → parser.py → chunker.py → dense.py + sparse.py →
                                          ↓
Query comes in → query.py → retrieve (dense + sparse) → fusion.py →
                                          ↓
                              reranker.py → context_assembly.py →
                                          ↓
                              llm.py generates answer → faithfulness.py checks it
```

---

## 1. ENTRY POINT

### `backend/app/main.py` (34 lines)

| Line | What it does |
|---|---|
| `lifespan()` | Prints active model name on startup — confirms which LLM is loaded. |
| `app = FastAPI(...)` | Creates the app, registers 3 routers (documents, query, evaluation). |
| `health()` | `GET /api/v1/health` — returns status, version, and active model name. |

**Interview:** "main.py is just the entry point — it wires up the three routers and prints which model is loaded on startup."

---

## 2. CONFIG

### `backend/app/core/config.py`

| Line | What it does |
|---|---|
| `class Settings(BaseSettings)` | Pydantic model that loads all config from `.env`. |
| `embedding_dim` property | Returns 384 for fastembed, 1536 for OpenAI. |
| `active_llm_model` property | Returns the actual model name in use (Groq or OpenAI). |
| `active_embedding_model` property | Returns the actual embedding model name in use. |

**Interview:** "All config lives in `.env`, loaded once by Pydantic. The `active_*` properties prevent the bug where the API reports the wrong model."

---

## 3. FACTORIES

### `backend/app/core/llm.py` (32 lines)

| Line | What it does |
|---|---|
| `get_llm()` | Returns a LlamaIndex LLM — `Groq(model=...)` if provider is "groq", `OpenAI(model=...)` if "openai". |

**Interview:** "One factory function. Returns the right LLM based on `LLM_PROVIDER` in `.env`. Groq for free, OpenAI when billing is added."

### `backend/app/core/embeddings.py` (31 lines)

| Line | What it does |
|---|---|
| `get_embed_model()` | Returns a LlamaIndex embed model — `FastEmbedEmbedding(...)` if "fastembed", `OpenAIEmbedding(...)` if "openai". |

**Interview:** "Same pattern as LLM. fastembed runs locally (zero cost, 384d). OpenAI is a fallback."

---

## 4. UPLOAD + INGESTION

### `backend/app/api/routes/documents.py` (124 lines)

| Function | What it does |
|---|---|
| `_ingest_sync(file_path, doc_id, filename)` | **The ingestion pipeline in one function**: parse → chunk → embed → index into Qdrant → build BM25 index. |
| `upload_document(file)` | `POST /api/v1/documents/upload` — saves file, calls `_ingest_sync`, returns task_id + status. |
| `document_status(doc_id)` | `GET /api/v1/documents/{id}/status` — always returns "completed" (sync ingestion). |

**Interview:** "`_ingest_sync` is the core. It calls `parse_document_pages` → `chunk_pages` → `VectorStoreIndex.from_documents` (embeds + stores in Qdrant) → `build_global_bm25`. One function, no async needed."

### `backend/app/ingestion/parser.py` (89 lines)

| Function | What it does |
|---|---|
| `parse_document(file_path)` | Reads PDF/DOCX/TXT → returns plain text string. |
| `parse_document_pages(file_path)` | Reads PDF → returns `[{"text": page1, "page_number": 1}, ...]`. Keeps page numbers for citations. |
| `_parse_pdf(file_path)` | Uses `pypdf` to extract text from each PDF page, joins with `\n\n`. |
| `_parse_pdf_pages(file_path)` | Same, but returns per-page dicts with page numbers. |
| `_parse_docx(file_path)` | Uses `python-docx` to extract paragraph text. |

**Interview:** "`parse_document_pages` is the one we use — it preserves page numbers so citations like `[1] DPO.pdf (p.5)` work later."

### `backend/app/ingestion/chunker.py` (230 lines)

| Function | What it does |
|---|---|
| `sentence_window_chunker(text, chunk_size=512, overlap=50)` | Splits text into 512-token chunks with 50-token overlap using LlamaIndex's `SentenceSplitter`. |
| `contextual_chunker(pages, chunk_size, overlap, llm)` | **Our main chunker**: detects sections via regex → summarizes each section with LLM → prepends `[Section: name — summary]` to each chunk. Makes every chunk self-contained. |
| `_detect_sections(text)` | Regex that finds section headers (Abstract, Introduction, Method, etc.) and splits text at those boundaries. |
| `_summarize_sections(sections, llm)` | Sends all section previews in ONE LLM call → returns dict mapping section name → one-sentence summary. |
| `chunk_pages(pages, strategy, ...)` | Router: if strategy is "contextual" → `contextual_chunker`, else → `sentence_window_chunker`. Called by `_ingest_sync`. |
| `SECTION_PATTERN` | Regex matching paper section headers: "Abstract", "Introduction", "1. Setup", "III. Results", etc. |

**Interview:** "Contextual chunking is the key innovation. A chunk cut mid-section loses context. We ask the LLM to summarize each section, then prepend that summary to every chunk in the section. This means each chunk's embedding captures the section's meaning, not just the raw text."

---

## 5. RETRIEVAL

### `backend/app/retrieval/dense.py` (40 lines)

| Function | What it does |
|---|---|
| `get_qdrant_client()` | Returns a singleton `QdrantClient` — in-memory if `QDRANT_URL=:memory:`, else connects to a server. |
| `ensure_collection(client)` | Creates the Qdrant collection if it doesn't exist. Sets vector size from `embedding_dim` and uses COSINE distance. |

**Interview:** "Singleton pattern so we don't create a new client on every request. `ensure_collection` is idempotent — safe to call multiple times."

### `backend/app/retrieval/sparse.py` (61 lines)

| Function | What it does |
|---|---|
| `class BM25Retriever` | Wraps `rank_bm25.BM25Okapi`. Tokenizes text with NLTK, builds inverted index. |
| `BM25Retriever.index(chunks)` | Tokenizes all chunk texts, builds the BM25 index. Called once at ingestion. |
| `BM25Retriever.retrieve(query, top_k=20)` | Scores all chunks against query, returns top_k with scores. |
| `build_global_bm25(chunks)` | Creates and stores the global BM25 index. Called by `_ingest_sync` after every upload. |
| `get_global_bm25()` | Returns the global index, or `None` if no documents have been uploaded yet. |

**Interview:** "BM25 is a global singleton built at ingestion time. It uses NLTK for tokenization and `BM25Okapi` for scoring. It catches exact terms that embeddings miss — acronyms, paper names, method IDs."

### `backend/app/retrieval/fusion.py` (34 lines)

| Function | What it does |
|---|---|
| `reciprocal_rank_fusion(*ranked_lists, top_k=20)` | Merges multiple ranked lists using RRF. Score = `sum(1/(rank + K))` for each list. Uses text prefix as dedup key. `K=60`. |

**Interview:** "RRF is score-free — it only uses ranks, not raw scores. This lets us combine dense (cosine similarity) and sparse (BM25 score) without normalizing. K=60 is the standard constant from the original paper."

---

## 6. RERANKING

### `backend/app/retrieval/reranker.py` (67 lines)

| Function | What it does |
|---|---|
| `get_cohere_client()` | Singleton `cohere.Client` using the API key from `.env`. |
| `_rate_limit()` | Sleeps if needed to keep under Cohere's 10-calls/minute trial limit (6.1s between calls). |
| `rerank(query, documents, top_k=5)` | Sends query + documents to Cohere `rerank-english-v3.0` cross-encoder → returns top_k with new relevance scores. Falls back to input order if API fails. |

**Interview:** "Cross-encoder sees query + document together, so it's more accurate than cosine similarity but slower. The rate limiter is critical for the trial tier — without it, every other call fails with 429."

---

## 7. QUERY TRANSFORMATION

### `backend/app/retrieval/query_transform.py` (41 lines)

| Function | What it does |
|---|---|
| `expand_query(query, num_variants=2)` | Asks LLM to rewrite the question in 2 different ways. Returns `[original, variant1, variant2]`. Used to improve recall — different phrasings retrieve different chunks. |

**Interview:** "Query expansion is optional (toggled via `expand` param). It rewrites the query with synonyms and alternative phrasings, then retrieves for all variants and re-fuses. It helps recall but costs an extra LLM call."

---

## 8. CONTEXT ASSEMBLY

### `backend/app/retrieval/context_assembly.py` (113 lines)

| Function | What it does |
|---|---|
| `deduplicate(chunks, threshold=0.85)` | Removes near-duplicate chunks using `SequenceMatcher`. Keeps the one with the higher score. |
| `order_chunks(chunks, strategy)` | `"reversed"` = most relevant first (placed last in prompt, closest to question). `"forward"` = most relevant last. `"original"` = no reorder. |
| `label_sources(chunks, start_index=1)` | Formats chunks as `[1] filename.pdf (p.5): chunk text...` for the LLM prompt. |
| `assemble_context(chunks, dedup, order, max_chunks, with_labels)` | **Full pipeline**: dedup → order → limit to max_chunks → label → join into one string. |
| `format_answer_with_citations(answer, chunks)` | Safety net — currently just returns the answer (citations handled by prompt). |

**Interview:** "`assemble_context` is what builds the final context string the LLM sees. Dedup removes near-identical chunks from fusion overlap. Ordering handles lost-in-the-middle. Labels give the LLM the `[1], [2]` markers for citations."

---

## 9. QUERY ENDPOINT

### `backend/app/api/routes/query.py`

| Function | What it does |
|---|---|
| `_dense_retrieve(index, query, top_k)` | Wrapper: retrieves from Qdrant via LlamaIndex `VectorIndexRetriever`. |
| `_hybrid_retrieve(query, top_k=20)` | Dense + BM25 + RRF fusion in one call. |
| `_generate_answer(query, context, llm)` | Sends prompt "Answer based on context... Cite sources using [1], [2]..." to the LLM. |
| `query(req)` | **The main endpoint** `POST /api/v1/query`. Routes to the right pipeline based on `config` param. |
| `query_stream(req)` | `POST /api/v1/query/stream` — same as `query` but returns SSE stream. |

**Config routing inside `query()`:**
- `vector_only` → dense retriever → top_k → LLM
- `vector_rerank` → dense retriever (top 20) → Cohere rerank (top 5) → LLM
- `hybrid` → dense + BM25 + RRF (top 5) → LLM
- `hybrid_rerank` → dense + BM25 + RRF (top 20) → Cohere rerank (top 5) → LLM
- `long_context` → fetch ALL chunks from Qdrant → truncate to 100k chars → LLM

**Interview:** "The query endpoint is a router. The `config` parameter selects which pipeline to run. Each config is a different combination of retrieve → fuse → rerank → generate. This lets us benchmark them against each other."

---

## 10. EVALUATION

### `backend/app/evaluation/configs.py` (145 lines)

| Function | What it does |
|---|---|
| `vector_only(question)` | Config 1: dense retrieval only, top-5, generate answer. |
| `vector_rerank(question)` | Config 2: dense top-20 → Cohere rerank top-5 → generate. |
| `hybrid(question)` | Config 3: dense + BM25 + RRF, top-5, no rerank. |
| `hybrid_rerank(question)` | Config 4: dense + BM25 + RRF top-20 → Cohere rerank top-5 → generate. |
| `long_context(question)` | Config 5: no retrieval — stuff ALL chunks (up to 100k chars) into prompt. |
| `CONFIGS` dict | Registry mapping config name → function. Used by the evaluation runner. |

**Interview:** "These are standalone functions — same logic as the query endpoint but simplified for batch evaluation. Each returns `(answer, contexts)` so the judge can score them."

### `backend/app/evaluation/ragas_runner.py` (261 lines)

| Function | What it does |
|---|---|
| `load_test_set(path)` | Loads 30 Q&A pairs from `data/eval/test_set.json`. |
| `run_evaluation(config_name, max_questions)` | Runs all questions through one config, scores with LLM-as-judge. |
| `score_faithfulness(answer, contexts, llm)` | LLM judges: are all claims in the answer supported by context? Returns 0.0-1.0. |
| `score_answer_relevancy(answer, question, llm)` | LLM judges: does the answer actually address the question? |
| `score_context_precision(answer, contexts, llm)` | LLM judges: are the retrieved contexts relevant to the answer? |
| `score_context_recall(ground_truth, contexts, llm)` | LLM judges: do the contexts contain the ground-truth information? |

**Interview:** "This is our custom LLM-as-judge — NOT the external RAGAs library. We call the same Groq LLM with specific prompts for each metric. Full control over methodology."

### `backend/app/evaluation/faithfulness.py` (74 lines)

| Function | What it does |
|---|---|
| `check_faithfulness(answer, contexts)` | **Runs on every live query.** Extracts claims from the answer, checks each against contexts (truncated to 500 chars), returns score + unsupported claims list. |

**Interview:** "This is the runtime guardrail. Every `/query` response includes a faithfulness score. It truncates contexts to 500 chars for the judge — that's a known limitation that lowers the score."

---

## 11. MODELS (Pydantic schemas)

### `backend/app/models/query.py`

| Model | What it defines |
|---|---|
| `QueryRequest` | Input: `question`, `config` (default "hybrid_rerank"), `top_k` (default 5), `expand` (bool). |
| `QueryResponse` | Output: `answer`, `sources[]`, `metadata{}`, `faithfulness{}`. |
| `Source` | One source chunk: `text`, `score`, `chunk_id`, `filename`, `page`. |
| `FaithfulnessCheck` | `score`, `supported_claims`, `total_claims`, `unsupported_claims[]`. |
| `Metadata` | `config`, `latency_ms`, `model`, `num_queries`, `num_sources`. |

### `backend/app/models/document.py`

| Model | What it defines |
|---|---|
| `UploadResponse` | `task_id`, `filename`, `status`. |
| `DocumentStatus` | `task_id`, `status`, `doc_id`. |

---

## Quick Reference: "What does this file do?"

| File | One sentence |
|---|---|
| `main.py` | Wires up routers, prints active model on startup. |
| `config.py` | Loads all `.env` settings into a Pydantic model. |
| `llm.py` | Factory — returns Groq or OpenAI LLM. |
| `embeddings.py` | Factory — returns fastembed or OpenAI embed model. |
| `documents.py` | Upload endpoint → parse → chunk → embed → index → build BM25. |
| `parser.py` | Reads PDF/DOCX/TXT, extracts text with page numbers. |
| `chunker.py` | Splits text into chunks; contextual mode prepends LLM section summaries. |
| `dense.py` | Qdrant singleton client + collection creation. |
| `sparse.py` | BM25 index — built once at ingestion, queried at retrieval. |
| `fusion.py` | RRF — merges dense + sparse rankings into one list. |
| `reranker.py` | Cohere cross-encoder — reorders chunks by true relevance. |
| `query_transform.py` | Expands a query into 2-3 variants for better recall. |
| `context_assembly.py` | Dedup → order → label → join chunks into one context string. |
| `query.py` | Main endpoint — routes to the right config pipeline. |
| `configs.py` | 5 standalone pipeline functions for batch evaluation. |
| `ragas_runner.py` | LLM-as-judge: scores faithfulness, relevancy, precision, recall. |
| `faithfulness.py` | Runtime claim-check on every live query response. |
