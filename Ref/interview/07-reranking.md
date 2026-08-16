# Lesson 7 — Reranking: Cohere Cross-Encoder

## What this lesson covers

- What a reranker is and why it is used
- The difference between bi-encoder and cross-encoder
- How your Cohere reranker is implemented
- The rate-limit handling and fallback

## Why rerank?

Dense retrieval and BM25 are fast because they use precomputed embeddings or inverted indexes. But they are **approximate**: they score documents independently of the query in a simple way.

A reranker is a slower, more accurate model that takes the query and each candidate document together and scores how relevant the document is to the query. It reranks the top-k candidates.

## Bi-encoder vs cross-encoder

| | Bi-encoder | Cross-encoder |
|---|---|---|
| When used | Dense retrieval | Reranking |
| Query/doc handling | Encoded independently | Encoded together |
| Speed | Fast (precomputed embeddings) | Slow (one forward pass per query-doc pair) |
| Accuracy | Good | Better |

Your dense retrieval uses a bi-encoder (fastembed). Your reranker uses a cross-encoder (Cohere `rerank-english-v3.0`).

## The reranker implementation

File: `backend/app/retrieval/reranker.py`

```python
_last_rerank_call = 0.0

def _rate_limit():
    global _last_rerank_call
    elapsed = time.time() - _last_rerank_call
    if elapsed < 6.1:
        time.sleep(6.1 - elapsed)
    _last_rerank_call = time.time()


def rerank(query, documents, top_k=5, model="rerank-english-v3.0"):
    if not documents:
        return []

    texts = [d["text"] for d in documents]

    try:
        _rate_limit()
        client = get_cohere_client()
        response = client.rerank(
            model=model,
            query=query,
            documents=texts,
            top_n=top_k,
        )
        reranked = []
        for r in response.results:
            idx = r.index
            result = dict(documents[idx])
            result["score"] = r.relevance_score
            reranked.append(result)
        return reranked

    except Exception as e:
        print(f"Cohere Rerank failed ({e}), falling back to top-{top_k} by input order")
        return documents[:top_k]
```

What it does:
1. Throttles calls to one every 6.1 seconds to stay under the Cohere trial rate limit (10 calls/minute).
2. Sends the query and the candidate documents to Cohere.
3. Cohere returns a relevance score for each document.
4. Returns the top-k reranked documents, preserving all metadata.
5. If anything fails, falls back to returning the original top-k input order.

## Why 6.1 seconds?

Cohere trial tier: 10 calls per minute.

`60 seconds / 10 calls = 6 seconds per call`. Adding 0.1 second buffer gives 6.1 seconds.

This is a real free-tier constraint that you engineered around.

## The graceful fallback

If the Cohere API call fails (rate limit, network error, key issue), the function returns `documents[:top_k]`. This keeps the system running even when the reranker is unavailable.

## Why this matters in an interview

You can say:

> "After dense and sparse retrieval, we optionally rerank the top candidates using Cohere's cross-encoder. A cross-encoder scores the query and document together, which is more accurate than the bi-encoder used in dense retrieval. Because we're on a Cohere trial key limited to 10 calls per minute, the code enforces a 6.1-second minimum gap and falls back to the original order if the call fails."

## Common trap

**"Is the reranker always better than no reranker?"**

Strong answer: no. Your own numbers show that `hybrid_rerank` (P@5 = 0.853) underperforms `hybrid` (P@5 = 0.953). More pipeline stages are not automatically better. The cross-encoder's notion of relevance does not perfectly match the paper-level relevance used in your evaluation. Stacking techniques must be validated, not assumed.

## Self-check

1. What is the difference between a bi-encoder and a cross-encoder?
2. Why is the reranker slower than dense retrieval?
3. What model does your reranker use?
4. Why is there a 6.1-second delay between calls?
5. What happens if the Cohere call fails?
6. Does your data show that reranking always improves retrieval? Explain.

## Code map

| Concept | File |
|---|---|
| Cohere client setup | `backend/app/retrieval/reranker.py` |
| Rate limiter | `backend/app/retrieval/reranker.py` |
| Rerank function | `backend/app/retrieval/reranker.py` |
| Fallback behavior | `backend/app/retrieval/reranker.py` |
