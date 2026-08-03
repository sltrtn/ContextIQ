"""Retrieval-only pipeline configs and metrics.

Used for computing IR metrics (Precision@k, Recall@k, MRR) without LLM calls.
"""

import json

from app.core.config import get_settings
from app.core.embeddings import get_embed_model
from app.retrieval.dense import get_qdrant_client, ensure_collection
from app.retrieval.sparse import get_global_bm25
from app.retrieval.fusion import reciprocal_rank_fusion
from app.retrieval.reranker import rerank
from llama_index.core import VectorStoreIndex
from llama_index.vector_stores.qdrant import QdrantVectorStore
from llama_index.core.retrievers import VectorIndexRetriever


def _get_index():
    """Helper to get the VectorStoreIndex from Qdrant."""
    settings = get_settings()
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)
    vector_store = QdrantVectorStore(client=qdrant, collection_name=settings.qdrant_collection)
    return VectorStoreIndex.from_vector_store(vector_store=vector_store, embed_model=get_embed_model())


def retrieve_vector_only(question: str, top_k: int = 5) -> list[dict]:
    """Dense retrieval only."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=top_k)
    nodes = retriever.retrieve(question)
    return [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id,
         "filename": n.node.metadata.get("filename"), "page_number": n.node.metadata.get("page_number")}
        for n in nodes
    ]


def retrieve_vector_rerank(question: str, top_k: int = 5) -> list[dict]:
    """Dense + Cohere Rerank."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    nodes = retriever.retrieve(question)
    dense_results = [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id,
         "filename": n.node.metadata.get("filename"), "page_number": n.node.metadata.get("page_number")}
        for n in nodes
    ]
    return rerank(question, dense_results, top_k=top_k)


def retrieve_hybrid(question: str, top_k: int = 5) -> list[dict]:
    """Dense + BM25 + RRF, no reranking."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    nodes = retriever.retrieve(question)
    dense_results = [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id,
         "filename": n.node.metadata.get("filename"), "page_number": n.node.metadata.get("page_number")}
        for n in nodes
    ]
    bm25 = get_global_bm25()
    sparse_results = bm25.retrieve(question, top_k=20) if bm25 else []
    return reciprocal_rank_fusion(dense_results, sparse_results, top_k=top_k)


def retrieve_hybrid_rerank(question: str, top_k: int = 5) -> list[dict]:
    """Dense + BM25 + RRF + Cohere Rerank."""
    fused = retrieve_hybrid(question, top_k=20)
    return rerank(question, fused, top_k=top_k)


def retrieve_long_context(question: str) -> list[dict]:
    """All chunks."""
    settings = get_settings()
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)
    all_points, _ = qdrant.scroll(
        collection_name=settings.qdrant_collection,
        limit=10000,
        with_payload=True,
    )
    results = []
    for p in all_points:
        text = p.payload.get("text")
        if not text and "_node_content" in p.payload:
            try:
                node_data = json.loads(p.payload["_node_content"])
                text = node_data.get("text", "")
            except Exception:
                text = ""
        if text:
            results.append({
                "text": text,
                "score": 0.0,
                "chunk_id": p.id,
                "filename": p.payload.get("filename"),
                "page_number": p.payload.get("page_number"),
            })
    return results


RETRIEVAL_CONFIGS = {
    "vector_only": retrieve_vector_only,
    "vector_rerank": retrieve_vector_rerank,
    "hybrid": retrieve_hybrid,
    "hybrid_rerank": retrieve_hybrid_rerank,
    "long_context": retrieve_long_context,
}


def is_relevant(result: dict, target_paper: str) -> bool:
    """Check if a retrieved result is from the target paper."""
    return result.get("filename") == target_paper


def compute_metrics(results: list[dict], target_paper: str, k: int = 5, total_relevant_in_corpus: int | None = None) -> dict:
    """Compute P@k, R@k, MRR for a single query.

    Args:
        results: ranked list of retrieved chunks
        target_paper: filename of the paper the question targets
        k: cutoff for precision/recall
        total_relevant_in_corpus: total number of chunks from target_paper in the corpus.
            If None, computed from `results` (only meaningful if results contains all chunks).
    """
    top_k = results[:k]
    relevant_in_top_k = sum(1 for r in top_k if is_relevant(r, target_paper))

    if total_relevant_in_corpus is None:
        # Fallback: compute from results. Only meaningful for full-corpus retrieval.
        total_relevant = sum(1 for r in results if is_relevant(r, target_paper))
    else:
        total_relevant = total_relevant_in_corpus

    precision_at_k = relevant_in_top_k / k if k > 0 else 0.0
    recall_at_k = relevant_in_top_k / total_relevant if total_relevant > 0 else 0.0

    # MRR: reciprocal rank of first relevant chunk
    mrr = 0.0
    for rank, r in enumerate(results, start=1):
        if is_relevant(r, target_paper):
            mrr = 1.0 / rank
            break

    return {
        "precision_at_k": precision_at_k,
        "recall_at_k": recall_at_k,
        "mrr": mrr,
        "relevant_in_top_k": relevant_in_top_k,
        "total_relevant": total_relevant,
    }
