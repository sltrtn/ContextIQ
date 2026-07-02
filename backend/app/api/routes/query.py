import asyncio
import time

from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from llama_index.core import VectorStoreIndex, Document as LlamaDocument
from llama_index.core.query_engine import RetrieverQueryEngine
from llama_index.core.retrievers import VectorIndexRetriever
from llama_index.embeddings.openai import OpenAIEmbedding
from llama_index.llms.openai import OpenAI
from llama_index.vector_stores.qdrant import QdrantVectorStore
from qdrant_client import QdrantClient

from app.core.config import get_settings
from app.models.query import QueryRequest, QueryResponse, Source
from app.retrieval.dense import get_qdrant_client, ensure_collection
from app.retrieval.sparse import BM25Retriever
from app.retrieval.fusion import reciprocal_rank_fusion
from app.retrieval.reranker import rerank

router = APIRouter(prefix="/api/v1", tags=["query"])
settings = get_settings()


def _naive_rag(query: str, top_k: int = 5) -> tuple[str, list[Source]]:
    """Direct dense retrieval, no reranking, no BM25."""
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)

    vector_store = QdrantVectorStore(
        client=qdrant,
        collection_name=settings.qdrant_collection,
    )
    embed_model = OpenAIEmbedding(model=settings.embedding_model)
    llm = OpenAI(model=settings.openai_model)

    index = VectorStoreIndex.from_vector_store(
        vector_store=vector_store,
        embed_model=embed_model,
    )
    retriever = VectorIndexRetriever(index=index, similarity_top_k=top_k)
    query_engine = RetrieverQueryEngine.from_args(
        retriever=retriever,
        llm=llm,
    )

    response = query_engine.query(query)
    sources = []
    for node in response.source_nodes:
        sources.append(Source(
            text=node.node.text,
            score=node.score or 0.0,
            doc_id=node.node.metadata.get("doc_id"),
            chunk_id=node.node_id,
        ))
    return str(response), sources


def _dense_only(query: str, top_k: int = 5) -> tuple[str, list[Source]]:
    """Dense retrieval + Cohere Rerank, no BM25."""
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)

    vector_store = QdrantVectorStore(
        client=qdrant,
        collection_name=settings.qdrant_collection,
    )
    embed_model = OpenAIEmbedding(model=settings.embedding_model)
    llm = OpenAI(model=settings.openai_model)

    index = VectorStoreIndex.from_vector_store(
        vector_store=vector_store,
        embed_model=embed_model,
    )
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    query_engine = RetrieverQueryEngine.from_args(
        retriever=retriever,
        llm=llm,
    )

    # Get dense results
    raw_response = retriever.retrieve(query)
    dense_results = [
        {"text": node.node.text, "score": node.score or 0.0, "chunk_id": node.node_id}
        for node in raw_response
    ]

    # Rerank
    reranked = rerank(query, dense_results, top_k=top_k)

    # Update sources with reranked context
    reranked_texts = [r["text"] for r in reranked]
    context = "\n\n".join(reranked_texts)
    response = llm.complete(
        f"Answer the question based on the context.\n\nContext:\n{context}\n\nQuestion: {query}"
    )

    sources = [
        Source(text=s["text"], score=s["score"], chunk_id=s.get("chunk_id"))
        for s in reranked
    ]
    return str(response), sources


@router.post("/query", response_model=QueryResponse)
async def query(req: QueryRequest):
    """Ask a question. Uses Hybrid + Rerank pipeline by default."""
    start = time.time()

    qdrant = get_qdrant_client()
    ensure_collection(qdrant)

    vector_store = QdrantVectorStore(
        client=qdrant,
        collection_name=settings.qdrant_collection,
    )
    embed_model = OpenAIEmbedding(model=settings.embedding_model)
    llm = OpenAI(model=settings.openai_model)

    index = VectorStoreIndex.from_vector_store(
        vector_store=vector_store,
        embed_model=embed_model,
    )

    # 1. Dense retrieval (top 20)
    retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
    dense_nodes = retriever.retrieve(req.question)
    dense_results = [
        {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id}
        for n in dense_nodes
    ]

    # 2. Sparse retrieval via BM25 (top 20)
    # For now, fetch all chunks from Qdrant to build BM25 index per query
    # In production this would use a persistent BM25 index
    all_chunks = [
        {"text": n.node.text, "node_id": n.node_id}
        for n in dense_nodes
    ]
    bm25 = BM25Retriever()
    bm25.index(all_chunks)
    sparse_results = bm25.retrieve(req.question, top_k=20)

    # 3. RRF fusion
    fused = reciprocal_rank_fusion(dense_results, sparse_results, top_k=20)

    # 4. Cohere Rerank → top 5
    reranked = rerank(req.question, fused, top_k=req.top_k)

    # 5. Generate answer
    context = "\n\n".join(r["text"] for r in reranked)
    response = llm.complete(
        f"Answer the question based on the context. Cite sources.\n\n"
        f"Context:\n{context}\n\nQuestion: {req.question}"
    )

    latency = time.time() - start

    sources = [
        Source(text=s["text"], score=s["score"], chunk_id=s.get("chunk_id"))
        for s in reranked
    ]

    return QueryResponse(
        answer=str(response),
        sources=sources,
        metadata={
            "latency_ms": round(latency * 1000, 2),
            "model": settings.openai_model,
            "dense_count": len(dense_results),
            "sparse_count": len(sparse_results),
            "reranked_count": len(reranked),
        },
    )


@router.post("/query/stream")
async def query_stream(req: QueryRequest):
    """Streaming version of /query using SSE."""

    async def event_stream():
        qdrant = get_qdrant_client()
        ensure_collection(qdrant)

        vector_store = QdrantVectorStore(
            client=qdrant,
            collection_name=settings.qdrant_collection,
        )
        embed_model = OpenAIEmbedding(model=settings.embedding_model)
        llm = OpenAI(model=settings.openai_model)

        index = VectorStoreIndex.from_vector_store(
            vector_store=vector_store,
            embed_model=embed_model,
        )

        retriever = VectorIndexRetriever(index=index, similarity_top_k=20)
        dense_nodes = retriever.retrieve(req.question)
        dense_results = [
            {"text": n.node.text, "score": n.score or 0.0, "chunk_id": n.node_id}
            for n in dense_nodes
        ]

        yield f"data: {{\"event\":\"dense\",\"count\":{len(dense_results)}}}\n\n"
        await asyncio.sleep(0.05)

        all_chunks = [
            {"text": n.node.text, "node_id": n.node_id}
            for n in dense_nodes
        ]
        bm25 = BM25Retriever()
        bm25.index(all_chunks)
        sparse_results = bm25.retrieve(req.question, top_k=20)

        yield f"data: {{\"event\":\"sparse\",\"count\":{len(sparse_results)}}}\n\n"
        await asyncio.sleep(0.05)

        fused = reciprocal_rank_fusion(dense_results, sparse_results, top_k=20)
        yield f"data: {{\"event\":\"fusion\",\"count\":{len(fused)}}}\n\n"
        await asyncio.sleep(0.05)

        reranked = rerank(req.question, fused, top_k=req.top_k)
        yield f"data: {{\"event\":\"rerank\",\"count\":{len(reranked)}}}\n\n"
        await asyncio.sleep(0.05)

        context = "\n\n".join(r["text"] for r in reranked)
        stream = llm.stream_complete(
            f"Answer the question based on the context. Cite sources.\n\n"
            f"Context:\n{context}\n\nQuestion: {req.question}"
        )

        for chunk in stream:
            yield f"data: {{\"event\":\"token\",\"text\":\"{chunk.delta.replace(chr(34), '\\\\\"')}\"}}\n\n"

        sources = [
            {"text": s["text"][:100], "score": s["score"]}
            for s in reranked
        ]
        yield f"data: {{\"event\":\"done\",\"sources\":{sources}}}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
    )
