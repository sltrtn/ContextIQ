import asyncio
import time

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from llama_index.core import VectorStoreIndex, Document as LlamaDocument
from llama_index.core.query_engine import RetrieverQueryEngine
from llama_index.core.retrievers import VectorIndexRetriever
from llama_index.vector_stores.qdrant import QdrantVectorStore
from qdrant_client import QdrantClient

from app.core.config import get_settings
from app.core.embeddings import get_embed_model
from app.core.llm import get_llm
from app.models.query import QueryRequest, QueryResponse, Source
from app.retrieval.dense import get_qdrant_client, ensure_collection
from app.retrieval.sparse import BM25Retriever, get_global_bm25
from app.retrieval.fusion import reciprocal_rank_fusion
from app.retrieval.reranker import rerank
from app.retrieval.context_assembly import assemble_context, deduplicate
from app.retrieval.query_transform import expand_query
from app.evaluation.faithfulness import check_faithfulness

router = APIRouter(prefix="/api/v1", tags=["query"])
settings = get_settings()


def _get_index():
    """Get VectorStoreIndex from Qdrant."""
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)
    vector_store = QdrantVectorStore(client=qdrant, collection_name=settings.qdrant_collection)
    return VectorStoreIndex.from_vector_store(vector_store=vector_store, embed_model=get_embed_model())


def _dense_retrieve(index, query: str, top_k: int = 20) -> list[dict]:
    """Dense retrieval from Qdrant."""
    retriever = VectorIndexRetriever(index=index, similarity_top_k=top_k)
    nodes = retriever.retrieve(query)
    return [
        {
            "text": n.node.text,
            "score": n.score or 0.0,
            "chunk_id": n.node_id,
            "filename": n.node.metadata.get("filename"),
            "page_number": n.node.metadata.get("page_number"),
        }
        for n in nodes
    ]


def _naive_rag(query: str, top_k: int = 5) -> tuple[str, list[Source]]:
    """Config: Direct dense retrieval, no reranking, no BM25."""
    index = _get_index()
    retriever = VectorIndexRetriever(index=index, similarity_top_k=top_k)
    query_engine = RetrieverQueryEngine.from_args(retriever=retriever, llm=get_llm())
    response = query_engine.query(query)
    sources = []
    for node in response.source_nodes:
        meta = node.node.metadata
        sources.append(Source(
            text=node.node.text,
            score=node.score or 0.0,
            doc_id=meta.get("doc_id"),
            chunk_id=node.node_id,
            filename=meta.get("filename"),
            page=meta.get("page_number"),
        ))
    return str(response), sources


def _dense_only(query: str, top_k: int = 5) -> tuple[str, list[Source]]:
    """Config: Dense retrieval + Cohere Rerank, no BM25."""
    index = _get_index()
    dense_results = _dense_retrieve(index, query, top_k=20)
    reranked = rerank(query, dense_results, top_k=top_k)
    context = assemble_context(reranked, dedup=False, order="original", max_chunks=top_k, with_labels=False)
    llm = get_llm()
    response = llm.complete(
        f"Answer the question based on the context.\n\nContext:\n{context}\n\nQuestion: {query}"
    )
    sources = [Source(text=s["text"], score=s["score"], chunk_id=s.get("chunk_id"), filename=s.get("filename"), page=s.get("page_number")) for s in reranked]
    return str(response), sources


def _hybrid_retrieve(query: str, top_k: int = 20) -> list[dict]:
    """Dense + BM25 + RRF fusion."""
    index = _get_index()
    dense_results = _dense_retrieve(index, query, top_k=top_k)
    bm25 = get_global_bm25()
    sparse_results = bm25.retrieve(query, top_k=top_k) if bm25 else []
    return reciprocal_rank_fusion(dense_results, sparse_results, top_k=top_k)


def _generate_answer(query: str, context: str, llm=None) -> str:
    """Generate answer from context."""
    if llm is None:
        llm = get_llm()
    response = llm.complete(
        f"Answer the question based on the context. Cite sources using [1], [2], etc.\n\n"
        f"Context:\n{context}\n\nQuestion: {query}"
    )
    return str(response)


@router.post("/query", response_model=QueryResponse)
async def query(req: QueryRequest):
    """Ask a question. Supports multiple pipeline configs via `config` parameter.

    Configs:
    - "hybrid_rerank" (default): Dense + BM25 + RRF + Cohere Rerank
    - "vector_only": Dense retrieval only, top_k
    - "vector_rerank": Dense + Cohere Rerank, no BM25
    - "hybrid": Dense + BM25 + RRF, no reranking
    - "long_context": All chunks, no retrieval
    """
    start = time.time()
    config = req.config or "hybrid_rerank"

    # Optional query expansion
    queries = [req.question]
    if req.expand:
        queries = expand_query(req.question)
        print(f"  Expanded to {len(queries)} queries")

    llm = get_llm()

    if config == "vector_only":
        index = _get_index()
        retriever = VectorIndexRetriever(index=index, similarity_top_k=req.top_k)
        nodes = retriever.retrieve(req.question)
        contexts = [n.node.text for n in nodes]
        context_str = "\n\n".join(contexts)
        answer = llm.complete(
            f"Answer the question based on the context.\n\nContext:\n{context_str}\n\nQuestion: {req.question}"
        )
        sources = [Source(text=n.node.text, score=n.score or 0.0, filename=n.node.metadata.get("filename"), page=n.node.metadata.get("page_number")) for n in nodes]

    elif config == "vector_rerank":
        index = _get_index()
        dense_results = _dense_retrieve(index, req.question, top_k=20)
        reranked = rerank(req.question, dense_results, top_k=req.top_k)
        context_str = assemble_context(reranked, dedup=False, order="original", max_chunks=req.top_k, with_labels=False)
        answer = llm.complete(
            f"Answer the question based on the context.\n\nContext:\n{context_str}\n\nQuestion: {req.question}"
        )
        sources = [Source(text=s["text"], score=s["score"], filename=s.get("filename"), page=s.get("page_number")) for s in reranked]

    elif config == "hybrid":
        fused = _hybrid_retrieve(req.question, top_k=20)
        top = fused[:req.top_k]
        context_str = assemble_context(top, dedup=True, order="reversed", max_chunks=req.top_k, with_labels=False)
        answer = llm.complete(
            f"Answer the question based on the context.\n\nContext:\n{context_str}\n\nQuestion: {req.question}"
        )
        sources = [Source(text=f["text"], score=f["score"], filename=f.get("filename"), page=f.get("page_number")) for f in top]

    elif config == "long_context":
        # Stuff all chunks into context
        qdrant = get_qdrant_client()
        ensure_collection(qdrant)
        all_points = qdrant.scroll(collection_name=settings.qdrant_collection, limit=10000, with_payload=True)[0]
        all_chunks = [{"text": p.payload.get("text", ""), "score": 0, "filename": p.payload.get("filename"), "page_number": p.payload.get("page_number")} for p in all_points if p.payload.get("text")]
        # Truncate to safe limit
        max_chars = 100000
        truncated = []
        total = 0
        for c in all_chunks:
            if total + len(c["text"]) > max_chars:
                break
            truncated.append(c)
            total += len(c["text"])
        context_str = "\n\n".join(c["text"] for c in truncated)
        answer = llm.complete(
            f"Answer the question based on the context.\n\nContext:\n{context_str}\n\nQuestion: {req.question}"
        )
        sources = [Source(text=c["text"][:200], score=0, filename=c.get("filename"), page=c.get("page_number")) for c in truncated[:5]]

    else:  # hybrid_rerank (default)
        # For multi-query: retrieve for each variant, merge
        all_fused = []
        for q in queries:
            fused = _hybrid_retrieve(q, top_k=20)
            all_fused.extend(fused)

        # Re-fuse across all query variants
        if len(queries) > 1:
            fused = reciprocal_rank_fusion(all_fused, top_k=20)
        else:
            fused = all_fused

        reranked = rerank(req.question, fused, top_k=req.top_k)
        context_str = assemble_context(reranked, dedup=True, order="reversed", max_chunks=req.top_k)
        answer = _generate_answer(req.question, context_str, llm)
        sources = [Source(text=s["text"], score=s["score"], filename=s.get("filename"), page=s.get("page_number")) for s in reranked]

    latency = time.time() - start

    # Faithfulness post-check
    context_texts = [s.text for s in sources]
    faith = check_faithfulness(str(answer), context_texts)

    return QueryResponse(
        answer=str(answer),
        sources=sources,
        metadata={
            "config": config,
            "latency_ms": round(latency * 1000, 2),
            "model": settings.active_llm_model,
            "num_queries": len(queries),
            "num_sources": len(sources),
        },
        faithfulness=faith,
    )


@router.post("/query/stream")
async def query_stream(req: QueryRequest):
    """Streaming version of /query using SSE."""

    async def event_stream():
        index = _get_index()
        llm = get_llm()

        dense_results = _dense_retrieve(index, req.question, top_k=20)
        yield f"data: {{\"event\":\"dense\",\"count\":{len(dense_results)}}}\n\n"
        await asyncio.sleep(0.05)

        bm25 = get_global_bm25()
        sparse_results = bm25.retrieve(req.question, top_k=20) if bm25 else []
        yield f"data: {{\"event\":\"sparse\",\"count\":{len(sparse_results)}}}\n\n"
        await asyncio.sleep(0.05)

        fused = reciprocal_rank_fusion(dense_results, sparse_results, top_k=20)
        yield f"data: {{\"event\":\"fusion\",\"count\":{len(fused)}}}\n\n"
        await asyncio.sleep(0.05)

        reranked = rerank(req.question, fused, top_k=req.top_k)
        yield f"data: {{\"event\":\"rerank\",\"count\":{len(reranked)}}}\n\n"
        await asyncio.sleep(0.05)

        context_str = assemble_context(reranked, dedup=True, order="reversed", max_chunks=req.top_k)
        stream = llm.stream_complete(
            f"Answer the question based on the context. Cite sources using [1], [2], etc.\n\n"
            f"Context:\n{context_str}\n\nQuestion: {req.question}"
        )

        for chunk in stream:
            yield f"data: {{\"event\":\"token\",\"text\":\"{chunk.delta.replace(chr(34), '\\\\\"')}\"}}\n\n"

        sources = [
            {"text": s["text"][:100], "score": s["score"], "filename": s.get("filename"), "page": s.get("page_number")}
            for s in reranked
        ]
        yield f"data: {{\"event\":\"done\",\"sources\":{sources}}}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
