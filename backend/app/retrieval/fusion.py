"""Reciprocal Rank Fusion (RRF) for combining sparse + dense results."""

RRF_K = 60  # standard constant


def reciprocal_rank_fusion(
    *ranked_lists: list[dict],
    top_k: int = 20,
) -> list[dict]:
    """Fuse multiple ranked lists using RRF.

    Each item must have at least a 'text' key.
    Scoring: RRF_score = sum(1 / (k + RRF_K)) for each occurrence.
    """
    scores: dict[str, float] = {}
    items: dict[str, dict] = {}

    for ranked in ranked_lists:
        for rank, item in enumerate(ranked, start=1):
            key = item["text"][:200]  # use text prefix as dedup key
            scores[key] = scores.get(key, 0.0) + 1.0 / (rank + RRF_K)
            if key not in items:
                items[key] = {
                    "text": item["text"],
                    "score": 0.0,
                    "chunk_id": item.get("chunk_id", ""),
                }

    # Apply fused scores
    for key, item in items.items():
        item["score"] = scores[key]

    # Sort by fused score descending
    fused = sorted(items.values(), key=lambda x: x["score"], reverse=True)
    return fused[:top_k]
