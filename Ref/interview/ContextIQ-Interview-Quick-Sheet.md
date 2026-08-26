# ContextIQ — TCS Prime Interview Quick Sheet (Last-Minute Revision)

> This is the 20% of the material that produces 80% of the questions. Memorize Section A cold. Skim the rest. Always bring answers back to YOUR code.

---

## SECTION A — The 6 questions you MUST nail

### 1. "Tell me about your project." (This is guaranteed. 90% of the interview hinges on it.)

> "I built **ContextIQ**, a production-grade RAG system that answers questions over research papers with **cited, page-accurate sources**. The problem: LLMs hallucinate on domain-specific papers and can't cite where an answer came from. My insight was that **retrieval quality is measurable** — so I built the whole pipeline around evaluation, not just demos.
>
> The flow is: **upload a PDF** → parse and chunk with contextual summaries → index into **Qdrant** (dense vectors) and a **BM25** sparse index → at query time, fuse both with **Reciprocal Rank Fusion** → rerank with a **Cohere cross-encoder** → generate an answer with **Groq openai/gpt-oss-120b** that cites sources like `[1] filename.pdf (p.5)`. Every live query also runs a **faithfulness check** so the user knows if the answer is grounded.
>
> I exposed 5 retrieval configs behind one endpoint, benchmarked all of them on a 30-question test set, and wrote a **custom LLM-as-judge** — no black-box RAGAs library. Stack: FastAPI, LlamaIndex, Qdrant, fastembed, Groq, Cohere, Docker Compose, and 39 pytest tests."

### 2. "What was the biggest technical challenge?" / "What was hard?"

Pick ONE of these (they are your strongest stories):

**A. Building an evaluation system, not just a demo.**
> "Most RAG tutorials stop at 'it answers questions.' I wanted numbers. I created a 30-question test set across 5 research papers, wrote a custom LLM-as-judge runner (`backend/app/evaluation/ragas_runner.py`), and measured every config on P@5, R@5, and MRR. The counter-intuitive finding: **more stages ≠ better**. `vector_rerank` beat `hybrid_rerank` on precision (0.9933 vs 0.8533) because adding BM25 + RRF before the reranker introduced noise the cross-encoder couldn't fully fix. That changed how I think about pipeline design — measure, don't assume."

**B. Making retrieval robust across question types.**
> "Dense embeddings are great for semantic similarity but miss exact terms — paper names, method acronyms like 'DPO', version numbers. I added a **BM25** sparse retriever (`backend/app/retrieval/sparse.py`) and fused it with dense via **Reciprocal Rank Fusion** (`backend/app/retrieval/fusion.py`). Then I added a **Cohere cross-encoder reranker** to reorder the fused results. The final default config is `hybrid_rerank`, but the eval showed me exactly when each stage helps."

**C. Citing sources accurately.**
> "A research QA system is useless if you can't trust the citation. I parse PDFs page-by-page (`backend/app/ingestion/parser.py`), preserve page metadata through chunking, and assemble context with source labels (`backend/app/retrieval/context_assembly.py`). The prompt explicitly tells the LLM to cite using `[1], [2], ...`, and every live response runs a faithfulness check that compares claims against the retrieved contexts."

### 3. "Why did you use X technology?" (table below covers all of them)

| Tech | Why (one-liner) |
|---|---|
| FastAPI | Async Python framework — handles file uploads and streaming responses cleanly. |
| LlamaIndex | Orchestrates parsing, chunking, vector indexing, and retrieval; pluggable components. |
| Qdrant | Vector database; supports in-memory dev and persistent deployment via Docker. |
| fastembed / BAAI/bge-small-en-v1.5 | Local embeddings, 384d, zero API cost, good enough for research papers. |
| Groq / openai/gpt-oss-120b | Fast, cheap inference on a capable model; free tier handles eval workloads. |
| Cohere rerank-english-v3.0 | Cross-encoder reranker — much more accurate than cosine similarity for ordering. |
| rank-bm25 | Lexical sparse retrieval — catches exact terms and acronyms embeddings miss. |
| RRF (K=60) | Combines dense + sparse rankings without training; robust fusion method. |
| Docker Compose | Reproducible stack: backend + persistent Qdrant in one command. |
| pytest | 39 tests covering chunking, retrieval, fusion, reranking, context assembly. |

### 4. "What is your role in the project?"

> "I designed and built the entire backend end-to-end — ingestion, retrieval, generation, evaluation, API, Docker setup, and tests. I made every architectural decision and can defend each one with measured results from the eval suite."

### 5. "What is the one-line pitch / what does it do?"

> "ContextIQ is a research-paper QA engine that retrieves cited sources and tells you how faithful the answer is."

### 6. "What would you improve / what's next?"

> "First, fix the faithfulness context window — it currently truncates retrieved chunks to 500 chars for the judge, which artificially lowers the score. Second, move from Qdrant in-memory to a persistent collection in Docker for production. Third, add query-relevance feedback so the system learns from thumbs-up/down. Fourth, add a React or Android frontend instead of just curl/API. Finally, more test coverage for the LLM-as-judge runner itself." — This shows self-awareness. Interviewers love honest limitations.

---

## SECTION B — RAG / AI fundamentals (very likely asked)

**Q: What is RAG?**
A: **Retrieval-Augmented Generation** — instead of asking an LLM to answer from its training memory, you retrieve relevant documents first, feed them into the prompt as context, and ask the model to answer from that context. Reduces hallucination and enables domain-specific QA.

**Q: What is an embedding?**
A: A dense numerical vector that captures semantic meaning. Sentences with similar meaning land close together in vector space. We use BAAI/bge-small-en-v1.5 (384 dimensions) from fastembed.

**Q: Dense vs. sparse retrieval?**
A: **Dense** retrieval uses vector similarity — good for semantic/paraphrase matching ("car" ≈ "vehicle"). **Sparse** retrieval (BM25) uses exact term matching — good for rare words, acronyms, names, and IDs. They complement each other; that is why we fuse them.

**Q: What is BM25?**
A: A lexical ranking function. It scores documents by term frequency, inverse document frequency, and document-length normalization. It is the sparse half of our hybrid retrieval.

**Q: What is Reciprocal Rank Fusion (RRF)?**
A: A score-free way to combine rankings from multiple retrievers. Each result gets `1 / (k + rank)` summed across lists; we use `K=60`. It does not need training and handles different score scales.

**Q: What is a cross-encoder reranker?**
A: A model that takes query + document together and outputs a relevance score. It is slower than bi-encoder retrieval but more accurate because it can attend to both texts jointly. We use Cohere rerank-english-v3.0.

**Q: What is contextual chunking?**
A: Instead of cutting text blindly, we ask an LLM to summarize the section a chunk belongs to, then prepend that summary to the chunk. This gives each chunk standalone meaning. Implemented in `backend/app/ingestion/chunker.py`.

**Q: What is "lost in the middle"?**
A: LLMs perform worse on information in the middle of a long context. Our context assembly orders chunks so the most relevant are at the **start** (and sometimes end) rather than buried in the middle.

**Q: What is an LLM-as-judge?**
A: Using an LLM to score outputs instead of human annotators. We use it for faithfulness, answer relevancy, context precision, and context recall in `backend/app/evaluation/ragas_runner.py`. It is our own implementation, not the external RAGAs library.

**Q: What is faithfulness?**
A: Whether every claim in the generated answer is supported by the retrieved context. We run a claim-level faithfulness check (`backend/app/evaluation/faithfulness.py`) on every live query.

**Q: What is hallucination?**
A: When the model generates information not present in the retrieved context. Faithfulness scoring catches this; citations also constrain the model.

**Q: What are P@5, R@5, and MRR?**
A:
- **Precision@5** = of the top-5 retrieved chunks, how many are actually relevant.
- **Recall@5** = of all relevant chunks in the document, how many appear in the top-5.
- **MRR** = Mean Reciprocal Rank — `1 / rank_of_first_relevant` averaged across questions.

**Q: What are the 5 pipeline configs?**
A:
1. `vector_only` — dense retrieval only.
2. `vector_rerank` — dense + Cohere rerank.
3. `hybrid` — dense + BM25 + RRF, no rerank.
4. `hybrid_rerank` (default) — dense + BM25 + RRF + Cohere rerank.
5. `long_context` — stuff all chunks into the LLM, no retrieval.

**Q: What were the measured results?**
A: On 30 questions across 5 papers:

| Config | P@5 | R@5 | MRR | Avg latency |
|---|---|---|---|---|
| vector_only | 0.9733 | 0.0996 | 0.9778 | ~3.8 s |
| vector_rerank | **0.9933** | 0.1016 | **1.0000** | ~178 s |
| hybrid | 0.9533 | 0.1000 | **1.0000** | ~3.6 s |
| hybrid_rerank | 0.8533 | 0.0931 | 0.9167 | ~179 s |
| long_context | 0.2000 | 0.0099 | 0.3987 | ~0.18 s |

Key takeaway: `vector_rerank` had the best precision and MRR, but cost ~178 s. `hybrid` gave near-perfect MRR at ~3.6 s without reranking. `hybrid_rerank` was slower *and* less accurate — the surprising result that justifies measuring.

---

## SECTION C — Backend / DevOps fundamentals (likely asked)

**Q: Why FastAPI?**
A: Modern async Python framework with automatic OpenAPI docs, Pydantic validation, file upload support, and native async/await for I/O-bound retrieval and LLM calls.

**Q: What is Qdrant?**
A: Vector database that stores embeddings and supports similarity search. We use it for dense retrieval. In dev it runs in-memory; Docker Compose gives persistent storage.

**Q: What is Docker Compose?**
A: Defines and runs multi-container apps. Our `docker-compose.yml` brings up the backend and Qdrant together with a persistent volume, so the stack is reproducible across machines.

**Q: What is pytest?**
A: Python testing framework. We have 39 tests covering chunking, parsing, retrieval, fusion, reranking, context assembly, and retrieval metrics.

**Q: What is Pydantic?**
A: Data validation library. We use it for request/response models like `QueryRequest` and `QueryResponse` in `backend/app/models/query.py`.

---

## SECTION D — Your design decisions (they will probe here)

**Q: "Why hybrid retrieval instead of just dense?"**
A: "Dense embeddings handle paraphrase and semantics, but they fail on exact terms like 'DPO', 'GSM-IC', or 'LoRA'. BM25 catches those. Fusing both gives broader coverage than either alone."

**Q: "Why did vector_rerank beat hybrid_rerank?"**
A: "Surprising result. Adding BM25 + RRF gave hybrid more recall breadth, but it also pushed noisier candidates into the top-20 that the Cohere reranker couldn't fully clean up. The lesson: more stages don't guarantee better results — you have to measure."

**Q: "Why contextual chunking?"**
A: "A chunk cut in the middle of a section can lose context. We prepend an LLM-generated section summary to each chunk so every chunk is self-contained. This improves retrieval quality because the embedding represents the chunk *plus* its context."

**Q: "Why custom LLM-as-judge instead of RAGAs?"**
A: "RAGAs is a black-box framework. I wanted full control over prompts, parsing, and metrics. My runner (`backend/app/evaluation/ragas_runner.py`) calls the same Groq LLM and computes faithfulness, answer relevancy, context precision, and context recall exactly the way I want."

**Q: "Why run faithfulness on every live query?"**
A: "Eval sets are for development; users are in production. A faithfulness score on every `/query` response tells the user immediately if the answer is grounded. It is a runtime guardrail."

**Q: "Why 512 token chunks / 50 overlap?"**
A: "Standard starting point. 512 tokens fits most embedding models and keeps chunks focused. 50-token overlap preserves continuity across chunk boundaries so sentences don't get split awkwardly."

**Q: "How would you scale this?"**
A: "(1) Persistent Qdrant with replicas, (2) separate ingestion workers for PDF parsing, (3) cache embeddings and BM25 index, (4) batch reranking calls to Cohere, (5) add a proper frontend and user auth, (6) move to a paid Groq/OpenAI tier for higher throughput."

---

## SECTION E — Behavioral (almost always asked)

**Q: Tell me about yourself.**
A: "I am a Computer Science student who builds end-to-end systems. I have two main projects: Meluko, a native Android social alarm clock built with Kotlin/Compose/Firebase, and ContextIQ, a retrieval-augmented AI backend that answers research-paper questions with cited sources and measured retrieval quality. I like shipping production-grade software and backing decisions with data."

**Q: Why do you want to join TCS Prime?**
A: "TCS Prime works on large-scale digital transformation for enterprise clients. I want to work on production systems with real users and learn how to scale reliable software. I have already shipped full-stack and AI projects end-to-end, so I am looking for an environment where I can go deeper on distributed systems and production engineering."

**Q: What is your strength?**
A: "End-to-end ownership. I design the architecture, write the code, add tests, containerize it, and ship it. With ContextIQ, I also built the evaluation suite so I can defend every pipeline decision with numbers."

**Q: What is a weakness?**
A: "I am still learning frontend deployment and production monitoring. ContextIQ currently has a strong backend but no polished UI or observability dashboard; I would add those next." (Turn a real gap into a next step.)

**Q: How do you handle conflict / a disagreement?**
A: "I listen first, then present data. In ContextIQ, every pipeline decision was decided by the eval metrics, not opinion. If the numbers say I am wrong, I change the design."

**Q: Where do you see yourself in 5 years?**
A: "Growing into a senior engineer who owns product features end-to-end, mentors juniors, and makes architecture decisions backed by measurement and production data."

---

## SECTION F — "Do you know anything we didn't cover?" (last question — always say yes)

Offer one of these briefly:
- How the 5-config ablation study was designed and what it revealed.
- The faithfulness post-check and why 500-char truncation affects the score.
- Contextual chunking and how section summaries improve retrieval.
- The trade-off between `vector_rerank` accuracy and `hybrid` latency.

---

## Rapid-fire recall (say these out loud before bed)

1. ContextIQ = research-paper RAG with cited, page-accurate answers.
2. Stack: FastAPI + LlamaIndex + Qdrant + fastembed + Groq openai/gpt-oss-120b + Cohere rerank.
3. Ingestion: pypdf/DOCX parsing → contextual chunking (512 tokens, 50 overlap) → Qdrant + BM25.
4. Retrieval: 5 configs — `vector_only`, `vector_rerank`, `hybrid`, `hybrid_rerank`, `long_context`.
5. Best P@5: `vector_rerank` = 0.9933; best speed/quality balance: `hybrid` = 3.6 s, MRR 1.0.
6. Fusion: RRF with K=60 on dense + BM25.
7. Reranker: Cohere rerank-english-v3.0 (cross-encoder).
8. Faithfulness: claim-level LLM-as-judge on every live query (`backend/app/evaluation/faithfulness.py`).
9. Eval: 30 questions × 5 configs, custom LLM-as-judge, NOT the RAGAs library.
10. Tests: 39 pytest tests; Docker Compose for reproducible deployment.

**Final tip:** For every answer, name the FILE (`query.py`, `chunker.py`, `ragas_runner.py`, `fusion.py`). It sounds like you own the code — because you do.
