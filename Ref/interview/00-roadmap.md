# ContextIQ — Interview Prep Roadmap

> Study guide series. Read in order. Each file is one lesson. Every concept is mapped to your actual code.

## The goal

You can explain ContextIQ from first principles, trace a request through the backend, and defend every architectural decision in an interview.

## Learning formats

| Format | Where |
|---|---|
| Written lessons | These `.md` files |
| Interactive Q&A | Live chat sessions |
| Flashcards | `13-flashcards.md` + `flashcard_quiz.py` (recite until automatic) |
| Mock interviews | Live chat rounds |

## The sequence

- [ ] **00 — Roadmap** (this file)
- [ ] **01 — Mental model** — RAG from zero: LLM, embedding, vector DB, dense vs sparse, ingest vs query
- [ ] **02 — Config and entry** — `main.py`, `config.py`, provider abstraction (`fastembed`/`openai`, `groq`/`openai`)
- [ ] **03 — Ingest and parse** — `documents.py` route, `parser.py`, upload flow, page metadata
- [ ] **04 — Chunking** — `chunker.py`: sentence-window, semantic, contextual, section regex
- [ ] **05 — Dense retrieval** — `embeddings.py`, `dense.py`, Qdrant, `VectorStoreIndex`
- [ ] **06 — Sparse and fusion** — `sparse.py` (BM25 global singleton), `fusion.py` (RRF, k=60)
- [ ] **07 — Reranking** — `reranker.py`: cross-encoder vs bi-encoder, rate limit, fallback
- [ ] **08 — Context assembly** — `context_assembly.py`: dedup, lost-in-the-middle ordering, source labels
- [ ] **09 — Query endpoint** — `query.py`: the 5 configs, query expansion, SSE stream, faithfulness hookup
- [ ] **10 — Evaluation** — `retrieval_metrics.py` (P@5, R@5, MRR), `faithfulness.py`, the numbers
- [ ] **11 — Judgment layer** — decisions log, traps, AI framing, the one-liner pitch
- [ ] **12 — Numbers table** — quick-reference facts to say cold
- [ ] **13 — Flashcards** — Q&A drills
- [ ] **14 — Corrections to Claude guide** — facts Claude got wrong

## How to use this

1. Read one file.
2. Close it and explain the concept out loud (to yourself or voice memo).
3. Open the referenced code file and trace the actual lines.
4. Try the self-check questions without looking.
5. Move on only when you can answer in plain English.

## The one rule

Every answer must connect to **your code**. Abstract definitions alone won't survive an interview follow-up.
