"""Cohere Rerank cross-encoder wrapper."""

import cohere

from app.core.config import get_settings

settings = get_settings()

_cohere_client: cohere.Client | None = None


def get_cohere_client() -> cohere.Client:
    global _cohere_client
    if _cohere_client is None:
        _cohere_client = cohere.Client(settings.cohere_api_key)
    return _cohere_client


def rerank(
    query: str,
    documents: list[dict],
    top_k: int = 5,
    model: str = "rerank-english-v3.0",
) -> list[dict]:
    """Re-rank documents using Cohere cross-encoder.

    Falls back to returning top_k by original score if rerank fails.
    """
    client = get_cohere_client()
    texts = [d["text"] for d in documents]

    try:
        response = client.rerank(
            model=model,
            query=query,
            documents=texts,
            top_n=top_k,
        )
        reranked = []
        for r in response.results:
            idx = r.index
            reranked.append({
                "text": texts[idx],
                "score": r.relevance_score,
                "chunk_id": documents[idx].get("chunk_id", ""),
            })
        return reranked

    except Exception as e:
        print(f"Cohere Rerank failed ({e}), falling back to top-{top_k} by input order")
        return documents[:top_k]
