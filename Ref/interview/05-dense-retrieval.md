# Lesson 5 — Dense Retrieval and Embeddings

## What this lesson covers

- How embeddings are produced in ContextIQ
- How dense retrieval works
- How Qdrant is used
- The difference between the embedding factory and the retrieval code

## Recap: embedding factory

File: `backend/app/core/embeddings.py`

```python
def get_embed_model():
    provider = settings.embedding_provider
    if provider == "fastembed":
        from llama_index.embeddings.fastembed import FastEmbedEmbedding
        return FastEmbedEmbedding(model_name=settings.fastembed_model)
    elif provider == "openai":
        from llama_index.embeddings.openai import OpenAIEmbedding
        return OpenAIEmbedding(model=settings.embedding_model)
```

This returns a model that can turn text into a vector. Your default is fastembed (`BAAI/bge-small-en-v1.5`, 384 dimensions).

## The Qdrant client

File: `backend/app/retrieval/dense.py`

```python
def get_qdrant_client():
    global _client
    if _client is not None:
        return _client
    if settings.qdrant_url == ":memory:":
        _client = QdrantClient(location=":memory:")
    else:
        kwargs = {"url": settings.qdrant_url}
        if settings.qdrant_api_key:
            kwargs["api_key"] = settings.qdrant_api_key
        _client = QdrantClient(**kwargs)
    return _client
```

What this does:
- Creates a singleton Qdrant client.
- If `QDRANT_URL` is `:memory:`, it uses an in-memory Qdrant instance (no Docker needed).
- Otherwise, it connects to a real Qdrant server (with optional API key).

## Ensuring the collection

```python
def ensure_collection(client):
    collections = client.get_collections().collections
    exists = any(c.name == settings.qdrant_collection for c in collections)
    if not exists:
        from qdrant_client.models import VectorParams, Distance
        client.create_collection(
            collection_name=settings.qdrant_collection,
            vectors_config=VectorParams(
                size=settings.embedding_dim,
                distance=Distance.COSINE,
            ),
        )
```

What this does:
- Checks if the collection named `contextiq_docs` exists.
- If not, creates it with a vector dimension equal to the active embedding provider.
- Uses **cosine distance** to compare vectors.

### Critical gotcha

The collection dimension must match the embedding provider.

- `fastembed` → 384 dimensions
- `openai` → 1536 dimensions

If you create a collection with 384-dim vectors and then switch to OpenAI, Qdrant will reject the 1536-dim vectors. Your `config.py` handles this via `embedding_dim`, but the collection would need to be recreated.

## Dense retrieval

File: `backend/app/api/routes/query.py`, function `_dense_retrieve`

```python
def _dense_retrieve(index, query: str, top_k: int = 20):
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
```

What this does:
- Creates a `VectorIndexRetriever` from a LlamaIndex `VectorStoreIndex`.
- Embeds the query into the same vector space as the chunks.
- Finds the `top_k` chunks whose vectors are closest to the query vector.
- Returns chunks with metadata: text, score, chunk_id, filename, page_number.

## Why it is called "dense"

Because every document and query is represented as a dense vector (a long list of numbers with no zero entries). This is in contrast to **sparse** representations like bag-of-words or BM25, where each dimension is a specific word and most values are zero.

## Bi-encoder vs cross-encoder

Dense retrieval uses a **bi-encoder**:
- The query is encoded independently into a vector.
- Each document is encoded independently into a vector.
- Similarity is computed as a dot product or cosine between the two vectors.

This is fast because document embeddings are precomputed at ingest time.

A **cross-encoder** (used in reranking) encodes the query and document together, which is more accurate but slower. Your reranker uses a cross-encoder.

## Why this matters in an interview

You can say:

> "Dense retrieval embeds the query and documents into the same vector space. We use fastembed for offline, zero-cost embeddings, and Qdrant to store and search them. The collection dimension is tied to the embedding provider, so switching from fastembed to OpenAI requires recreating the collection."

## Common trap

**"How does dense retrieval handle exact keywords?"**

Strong answer: dense retrieval can miss exact keyword matches because it searches by meaning, not word overlap. That is why your project also uses BM25 (sparse retrieval) and fuses the two.

## Self-check

1. What is the default embedding provider and how many dimensions does it produce?
2. What does `ensure_collection` do?
3. Why does the Qdrant collection dimension matter?
4. What distance metric does Qdrant use?
5. What is the difference between a bi-encoder and a cross-encoder?
6. What is one weakness of dense retrieval that your project addresses?

## Code map

| Concept | File |
|---|---|
| Embedding factory | `backend/app/core/embeddings.py` |
| Qdrant client | `backend/app/retrieval/dense.py` |
| Collection creation | `backend/app/retrieval/dense.py` |
| Dense retrieval | `backend/app/api/routes/query.py` |
| Vector store index | `backend/app/api/routes/query.py` |
