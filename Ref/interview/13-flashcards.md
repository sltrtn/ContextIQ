# Lesson 13 — Flashcards

Read the question, cover the answer, say the answer out loud. Repeat until automatic.

> **Interactive version:** Run `python Ref/interview/flashcard_quiz.py` for a terminal drill that keeps missed cards in a review pile.

## Mental model

**Q: What is RAG?**
A: Retrieval-Augmented Generation: retrieve relevant chunks, augment the LLM prompt with them, then generate an answer.

**Q: Why not just paste the whole document into the LLM?**
A: Context window limits, cost, and worse accuracy due to noise / lost-in-the-middle effect.

**Q: What is an embedding?**
A: Text converted into a vector (list of numbers) where similar meanings are close together.

**Q: What is a vector database?**
A: A database optimized to find stored vectors that are close to a query vector.

**Q: What is the difference between dense and sparse retrieval?**
A: Dense searches by semantic meaning (embeddings). Sparse searches by exact keyword match (BM25).

**Q: Why is fine-tuning not the right solution here?**
A: Expensive, requires retraining when documents change, and cannot cite sources.

## Config and entry

**Q: What web framework does ContextIQ use?**
A: FastAPI.

**Q: What is the default embedding provider?**
A: fastembed with `BAAI/bge-small-en-v1.5`, producing 384-dimensional vectors.

**Q: What is the default LLM provider?**
A: Groq with `llama-3.3-70b-versatile`.

**Q: Why are fastembed and Groq the defaults?**
A: The OpenAI key had no billing credits; fastembed and Groq are free for development.

**Q: What pattern do `embeddings.py` and `llm.py` use?**
A: The strategy / factory pattern — the provider is swappable without changing the rest of the code.

## Ingest and parse

**Q: What file types can be uploaded?**
A: PDF, DOCX, TXT.

**Q: What library parses PDFs?**
A: `pypdf`.

**Q: Why preserve page numbers during parsing?**
A: So the final answer can cite specific pages like `(p.5)`.

**Q: What is an honest limitation of `pypdf`?**
A: It is text-extraction only; it does not handle layout, figures, or tables well.

**Q: When is the BM25 index rebuilt?**
A: After every document upload, using all accumulated chunks.

## Chunking

**Q: What are the three chunking strategies?**
A: Sentence-window, semantic, contextual.

**Q: Which is the default?**
A: Contextual chunking.

**Q: How does contextual chunking work?**
A: Detect sections, summarize each with one batched LLM call, prepend `[Section: Name — Summary]` to chunks.

**Q: How many LLM calls does summarization use per document?**
A: One batched call.

**Q: What is the honest caveat about contextual chunking?**
A: It has not been A/B tested against plain sentence-window chunking in isolation.

## Dense retrieval

**Q: What is the default embedding dimension?**
A: 384.

**Q: What vector database is used?**
A: Qdrant.

**Q: What distance metric does Qdrant use?**
A: Cosine.

**Q: What is the difference between a bi-encoder and a cross-encoder?**
A: Bi-encoder encodes query and document independently (fast). Cross-encoder encodes them together (more accurate, slower).

**Q: What is one weakness of dense retrieval that your project addresses?**
A: It can miss exact keyword matches, so BM25 is used as a complement.

## Sparse and fusion

**Q: What is BM25?**
A: A keyword-based ranking function that scores documents by term frequency and document length.

**Q: Why is BM25 a global singleton built at ingest time?**
A: Because an earlier version rebuilt BM25 from dense results per query, which only searched the top-20 dense chunks instead of the full corpus.

**Q: What is RRF?**
A: Reciprocal Rank Fusion: merges ranked lists by summing `1 / (rank + k)` for each occurrence.

**Q: What is `RRF_K` in your code?**
A: 60.

**Q: Why does RRF use rank instead of raw score?**
A: Dense cosine similarity and BM25 scores are on different scales. Ranks are comparable.

## Reranking

**Q: What reranker model does ContextIQ use?**
A: Cohere `rerank-english-v3.0`.

**Q: Why is there a 6.1-second delay between rerank calls?**
A: Cohere trial tier is limited to 10 calls per minute.

**Q: What happens if the Cohere call fails?**
A: The reranker falls back to returning the top-k input documents in original order.

**Q: Does reranking always improve retrieval?**
A: No. `hybrid_rerank` underperforms `hybrid` and `vector_rerank` in the benchmark.

## Context assembly

**Q: What are the three jobs of context assembly?**
A: Deduplicate, order, label.

**Q: How does deduplication work?**
A: SequenceMatcher compares first 200 characters; if similarity > 0.85, keep the higher-scored chunk.

**Q: Why is the most relevant chunk placed last?**
A: LLMs attend better to text near the question at the end of the context.

**Q: What does a source label look like?**
A: `[1] filename.pdf (p.5): chunk text...`

## Query endpoint

**Q: What is the default config?**
A: `hybrid_rerank`.

**Q: List the five configs.**
A: `vector_only`, `vector_rerank`, `hybrid`, `hybrid_rerank`, `long_context`.

**Q: Which config performed best?**
A: `vector_rerank` (P@5 = 0.9933, MRR = 1.0).

**Q: Which config performed worst?**
A: `long_context` (P@5 = 0.20).

**Q: What does query expansion do?**
A: Rewrites the question into variants to improve recall, then fuses results.

**Q: When does the faithfulness check run?**
A: After the answer is generated.

## Evaluation

**Q: What does P@5 measure?**
A: Of the top-5 retrieved chunks, how many are relevant.

**Q: What does R@5 measure?**
A: Of all relevant chunks in the corpus, how many appear in the top-5.

**Q: What does MRR measure?**
A: The average reciprocal rank of the first relevant chunk across questions.

**Q: Why is recall low (~10%) despite high precision?**
A: Each paper has 20–173 relevant chunks; taking only the top-5 caps recall mechanically.

**Q: What is the headline insight from the benchmark?**
A: `hybrid_rerank` underperforms `vector_rerank` and `hybrid` — stacking techniques must be validated.

**Q: What is a limitation of the faithfulness check?**
A: It uses the same model family as the generator, so it can share blind spots.

## Judgment layer

**Q: Why did you build BM25 as a global singleton instead of per-query?**
A: Per-query rebuild from dense results only searched the top dense chunks, missing sparse-only matches.

**Q: What is the honest answer to "Is this in production?"**
A: No. Qdrant is in-memory, BM25 is lost on restart, and auth/rate limiting are not wired yet.

**Q: What is the correct statement about RAGAs?**
A: We do not use the RAGAs library. We built a custom LLM-as-judge because RAGAs had a broken import.

**Q: Say the one-line pitch.**
A: "Most RAG demos are judged by vibes — 'it gave a good answer.' I built ContextIQ to treat retrieval quality as a measurable engineering property: five retrieval configurations benchmarked on a 30-question test set across precision, recall, and MRR, with a faithfulness check on every live query."
