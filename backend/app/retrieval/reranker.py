"""Cohere Rerank cross-encoder wrapper."""

import time

import cohere

from app.core.config import get_settings

settings = get_settings()

_cohere_client: cohere.Client | None = None

# Simple rate-limiting guard: keep at least 6.1s between calls to stay under
# Cohere's trial-tier 10 calls/minute limit.
_last_rerank_call: float = 0.0


def get_cohere_client() -> cohere.Client:
    global _cohere_client
    if _cohere_client is None:
        _cohere_client = cohere.Client(settings.cohere_api_key)
    return _cohere_client


def _rate_limit():
    global _last_rerank_call
    elapsed = time.time() - _last_rerank_call
    if elapsed < 6.1:
        time.sleep(6.1 - elapsed)
    _last_rerank_call = time.time()


def rerank(
    query: str,
    documents: list[dict],
    top_k: int = 5,
    model: str = "rerank-english-v3.0",
) -> list[dict]:
    """Re-rank documents using Cohere cross-encoder.

    Falls back to returning top_k by original score if rerank fails.
    """
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
