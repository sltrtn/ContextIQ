"""Pipeline configurations for ablation studies.

Each config is a function that takes a question and returns (answer, contexts_list).
This allows running the same test set against different pipeline setups.
"""

from app.core.config import get_settings
from app.core.embeddings import get_embed_model
from app.core.llm import get_llm
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


def vector_only(question: str) -> tuple[str, list[str]]:
    """Config 1: Dense retrieval only, no reranking, top-5."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=5)
    nodes = retriever.retrieve(question)
    contexts = [n.node.text for n in nodes]
    llm = get_llm()
    context_block = "\n\n".join(contexts)
    response = llm.complete(
        f"Answer the question based on the context.\n\nContext:\n{context_block}\n\nQuestion: {question}"
    )
    return str(response), contexts


def vector_rerank(question: str) -> tuple[str, list[str]]:
    """Config 2: Dense retrieval + Cohere Rerank, no BM25, top-5."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    nodes = retriever.retrieve(question)
    dense_results = [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id}
        for n in nodes
    ]
    reranked = rerank(question, dense_results, top_k=5)
    contexts = [r["text"] for r in reranked]
    llm = get_llm()
    context_block = "\n\n".join(contexts)
    response = llm.complete(
        f"Answer the question based on the context.\n\nContext:\n{context_block}\n\nQuestion: {question}"
    )
    return str(response), contexts


def hybrid(question: str) -> tuple[str, list[str]]:
    """Config 3: Dense + BM25 + RRF fusion, no reranking, top-5."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    nodes = retriever.retrieve(question)
    dense_results = [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id}
        for n in nodes
    ]
    bm25 = get_global_bm25()
    sparse_results = bm25.retrieve(question, top_k=20) if bm25 else []
    fused = reciprocal_rank_fusion(dense_results, sparse_results, top_k=5)
    contexts = [f["text"] for f in fused]
    llm = get_llm()
    context_block = "\n\n".join(contexts)
    response = llm.complete(
        f"Answer the question based on the context.\n\nContext:\n{context_block}\n\nQuestion: {question}"
    )
    return str(response), contexts


def hybrid_rerank(question: str) -> tuple[str, list[str]]:
    """Config 4: Dense + BM25 + RRF + Cohere Rerank, top-5 (full pipeline)."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    nodes = retriever.retrieve(question)
    dense_results = [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id}
        for n in nodes
    ]
    bm25 = get_global_bm25()
    sparse_results = bm25.retrieve(question, top_k=20) if bm25 else []
    fused = reciprocal_rank_fusion(dense_results, sparse_results, top_k=20)
    reranked = rerank(question, fused, top_k=5)
    contexts = [r["text"] for r in reranked]
    llm = get_llm()
    context_block = "\n\n".join(contexts)
    response = llm.complete(
        f"Answer the question based on the context. Cite sources.\n\n"
        f"Context:\n{context_block}\n\nQuestion: {question}"
    )
    return str(response), contexts


def long_context(question: str) -> tuple[str, list[str]]:
    """Config 5: No retrieval — stuff ALL chunks into context window."""
    from app.retrieval.dense import get_qdrant_client, ensure_collection
    settings = get_settings()
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)

    # Fetch all points from Qdrant
    all_points = qdrant.scroll(
        collection_name=settings.qdrant_collection,
        limit=10000,
        with_payload=True,
    )[0]
    contexts = [p.payload.get("text", "") for p in all_points if p.payload.get("text")]

    llm = get_llm()
    # Truncate to avoid context window overflow (rough estimate: 1 token ~ 4 chars)
    max_chars = 100000  # ~25k tokens, safe for most models
    truncated = []
    total = 0
    for c in contexts:
        if total + len(c) > max_chars:
            break
        truncated.append(c)
        total += len(c)

    context_block = "\n\n".join(truncated)
    response = llm.complete(
        f"Answer the question based on the context.\n\nContext:\n{context_block}\n\nQuestion: {question}"
    )
    return str(response), truncated


# Registry of all configs
CONFIGS = {
    "vector_only": vector_only,
    "vector_rerank": vector_rerank,
    "hybrid": hybrid,
    "hybrid_rerank": hybrid_rerank,
    "long_context": long_context,
}
