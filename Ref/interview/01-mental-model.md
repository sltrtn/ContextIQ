# Lesson 1 — Mental Model: What is RAG and why does it exist?

## The problem ContextIQ solves

You have documents (research papers). You want to ask questions and get accurate answers that are **grounded in those documents and cite where they came from**.

The naive approach — paste the whole PDF into ChatGPT — fails because:

1. **LLMs have a context window.** They can only read a limited number of tokens at once.
2. **More tokens = more cost.** Sending a 20-page paper every time is expensive.
3. **More noise = worse answers.** LLMs struggle when the prompt is bloated with irrelevant text.

Your own `long_context` config proves this: it is the worst-performing retrieval config (P@5 = 0.20).

## The RAG pattern

RAG = **Retrieval-Augmented Generation**.

- **Retrieve** only the most relevant chunks of the document.
- **Augment** the LLM prompt with those chunks.
- **Generate** the answer, now grounded in the retrieved sources.

This is the entire architecture of ContextIQ.

## The four building blocks

### 1. LLM (Large Language Model)
- What it is: a program trained on huge amounts of text.
- What it does: predict the next token, given the tokens that came before.
- Everything else — writing, reasoning, coding — emerges from that one task.
- Analogy: an extremely well-read autocomplete.
- In your code: `backend/app/core/llm.py` — uses Groq's `openai/gpt-oss-120b`.

### 2. Embedding
- What it is: text converted into a list of numbers (a vector).
- Example: `[0.23, -0.41, 0.87, ...]` — 384 numbers in your case.
- Key property: similar meaning = similar vectors. Computers cannot compare meaning directly, but they can compare lists of numbers using cosine similarity.
- Analogy: GPS coordinates for meaning. "Cat" and "kitten" are close; "cat" and "quantum" are far.
- In your code: `backend/app/core/embeddings.py` — uses fastembed (`BAAI/bge-small-en-v1.5`, 384 dimensions).

### 3. Vector database
- What it is: a database designed to store vectors and answer "which stored vectors are closest to this query vector?" very fast.
- Why not a normal database: normal DBs search by exact match. Vector DBs search by semantic proximity.
- Analogy: a library shelved by meaning rather than by title.
- In your code: `backend/app/retrieval/dense.py` — Qdrant. In dev mode it runs in-memory (`:memory:`); in production it uses Docker with persistence.

### 4. Retrieval
- What it is: the act of finding relevant chunks given a query.
- Two complementary approaches:
  - **Dense retrieval** (vector search): finds meaning matches even if the words differ.
  - **Sparse retrieval** (BM25 / keyword): finds exact word matches.
- Analogy: dense = a librarian who understands topics; sparse = a keyword scanner.
- **Hybrid** = both merged together. Your project uses this in `hybrid` and `hybrid_rerank`.

## The two phases

### INGEST (per document, one time)

```
Upload PDF/DOCX/TXT
  → Parse into pages
  → Chunk into smaller pieces
  → Embed each chunk
  → Store in Qdrant
  → Build BM25 index
```

### QUERY (every question)

```
User asks a question
  → Embed the question
  → Retrieve top chunks (dense + sparse)
  → Fuse the two lists
  → Optionally rerank
  → Assemble into a prompt
  → LLM generates answer with citations
  → Faithfulness check
  → Return answer
```

Every file in `backend/app/` sits on one of these arrows.

## Why RAG and not the alternatives?

### Why not fine-tuning?
- Fine-tuning means retraining the model on your documents.
- It is expensive, needs ML expertise, and the knowledge becomes stale the moment the document changes.
- RAG keeps documents **external**: edit the document, re-ingest, and the system picks up the change instantly.
- RAG also lets you **cite sources** — fine-tuning cannot tell you which page the answer came from.

### Why not just stuff the whole document in the prompt?
- Context window limits, cost, and the **lost-in-the-middle** effect.
- LLMs pay less attention to information in the middle of a long context.
- Your `long_context` config is literally this alternative, and it is your worst-performing config.

### Why retrieval at all?
- Because LLMs hallucinate. They confidently make things up.
- Grounding the answer in retrieved chunks makes it **verifiable**.
- That is why your `faithfulness.py` re-checks every generated answer against its sources.

## Your interview opener

> "Most RAG demos are judged by vibes — 'it gave a good answer.' I built ContextIQ to treat retrieval quality as a measurable engineering property: five retrieval configurations benchmarked on a 30-question test set across precision, recall, and MRR, with a faithfulness check on every live query."

## Self-check

1. Explain RAG to a 12-year-old in your own words.
2. What is the difference between dense and sparse retrieval?
3. List three reasons you cannot just paste the whole PDF into the LLM.
4. Why is fine-tuning not the right solution for this problem?
5. What does it mean that "similar meaning = similar vectors"?

## Code map

| Concept | File |
|---|---|
| LLM factory | `backend/app/core/llm.py` |
| Embedding factory | `backend/app/core/embeddings.py` |
| Vector DB client | `backend/app/retrieval/dense.py` |
| Dense retrieval | `backend/app/retrieval/dense.py` |
| Sparse retrieval | `backend/app/retrieval/sparse.py` |
| Fusion | `backend/app/retrieval/fusion.py` |
| Faithfulness check | `backend/app/evaluation/faithfulness.py` |
| Full pipeline | `backend/app/api/routes/query.py` |
