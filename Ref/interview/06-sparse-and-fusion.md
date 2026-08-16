# Lesson 6 — Sparse Retrieval and Fusion

## What this lesson covers

- What BM25 is
- Why it is a global singleton built at ingest time
- How Reciprocal Rank Fusion (RRF) merges dense and sparse results
- Why RRF is used instead of averaging scores

## What is BM25?

BM25 is a classic keyword search algorithm. It scores documents based on how often the query terms appear in them, adjusted for document length.

- If a query contains a rare technical term, documents containing that exact term get a high score.
- It does not understand synonyms or paraphrases — it is literal.

BM25 is the complement to dense retrieval. Dense retrieval catches meaning; BM25 catches exact keyword matches.

## The BM25 implementation

File: `backend/app/retrieval/sparse.py`

```python
class BM25Retriever:
    def __init__(self):
        self._chunks = []
        self._tokenized = []
        self._bm25 = None

    def index(self, chunks):
        if not chunks:
            self._chunks = []
            self._tokenized = []
            self._bm25 = None
            return
        self._chunks = chunks
        self._tokenized = [word_tokenize(c["text"].lower()) for c in chunks]
        self._bm25 = BM25Okapi(self._tokenized)

    def retrieve(self, query, top_k=20):
        if self._bm25 is None:
            return []
        tokenized_query = word_tokenize(query.lower())
        scores = self._bm25.get_scores(tokenized_query)
        scored = list(enumerate(scores))
        scored.sort(key=lambda x: x[1], reverse=True)
        top = scored[:top_k]
        results = []
        for idx, score in top:
            result = dict(self._chunks[idx])
            result["score"] = float(score)
            result["chunk_id"] = self._chunks[idx].get("chunk_id", self._chunks[idx].get("node_id", ""))
            result["index"] = idx
            results.append(result)
        return results
```

What it does:
- Tokenizes each chunk into lowercase words using NLTK.
- Builds a BM25 index from the tokenized chunks.
- At query time, tokenizes the query and scores every chunk.
- Returns the top-k chunks.

## Global singleton

File: `backend/app/retrieval/sparse.py`

```python
_global_bm25 = None

def build_global_bm25(chunks):
    global _global_bm25
    _global_bm25 = BM25Retriever()
    _global_bm25.index(chunks)

def get_global_bm25():
    return _global_bm25
```

Why a global singleton?

Your earlier design rebuilt BM25 from the *dense* results for every query. That was wrong because BM25 needs to search the **full corpus** to catch sparse-only matches. Rebuilding from dense results only covered the top-20 dense chunks, which defeats the purpose of BM25.

Now the index is built once at ingest time and reused for every query.

### Honest limitation

BM25 is held in memory. If the server restarts, the index is lost and you must re-upload documents to rebuild it.

## Fusion: Reciprocal Rank Fusion (RRF)

File: `backend/app/retrieval/fusion.py`

```python
RRF_K = 60

def reciprocal_rank_fusion(*ranked_lists, top_k=20):
    scores = {}
    items = {}

    for ranked in ranked_lists:
        for rank, item in enumerate(ranked, start=1):
            key = item["text"][:200]
            scores[key] = scores.get(key, 0.0) + 1.0 / (rank + RRF_K)
            if key not in items:
                merged = dict(item)
                merged["score"] = 0.0
                items[key] = merged

    for key, item in items.items():
        item["score"] = scores[key]

    fused = sorted(items.values(), key=lambda x: x["score"], reverse=True)
    return fused[:top_k]
```

What it does:
- Takes multiple ranked lists (e.g., dense results and BM25 results).
- For each item, computes `sum(1 / (rank + 60))` across every list it appears in.
- Items that appear high in multiple lists get the highest fused score.
- Returns the top-k fused results.

### Why RRF and not weighted averaging?

Dense retrieval scores and BM25 scores live on completely different scales.

- Dense: cosine similarity, typically between 0 and 1.
- BM25: raw scores, can be anything from 0 to thousands.

You cannot simply add them. RRF ignores the raw scores and uses only **rank position**. Since ranks are comparable across lists, no normalization is needed.

This is the standard, textbook answer. If you only remember one thing about fusion, remember this:

> "RRF fuses on rank, not raw score, because dense cosine similarity and BM25 scores are on different scales."

## Why this matters in an interview

You can say:

> "We use BM25 as a sparse keyword complement to dense vector search. BM25 is built once at ingest time as a global singleton — earlier it was rebuilt from dense results per query, which was semantically wrong because BM25 needs to search the full corpus. The two lists are fused using Reciprocal Rank Fusion with k=60, which merges on rank position because the raw scores are not comparable."

## Common trap

**"Why not just add the dense and BM25 scores?"**

Strong answer: the scores are on different scales. Cosine similarity is bounded, BM25 scores are not. RRF sidesteps normalization by using only rank position.

## Self-check

1. What does BM25 do well that dense retrieval can miss?
2. What does dense retrieval do well that BM25 can miss?
3. Why is BM25 a global singleton built at ingest time?
4. What is the RRF formula?
5. What is `RRF_K` set to in your code?
6. Why does RRF use rank instead of raw score?
7. What happens to the BM25 index when the server restarts?

## Code map

| Concept | File |
|---|---|
| BM25 retriever | `backend/app/retrieval/sparse.py` |
| Global singleton | `backend/app/retrieval/sparse.py` |
| RRF fusion | `backend/app/retrieval/fusion.py` |
| RRF constant | `backend/app/retrieval/fusion.py` (line 3) |
| Hybrid retrieval | `backend/app/api/routes/query.py` |
