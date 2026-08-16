# Lesson 2 — Config and Entry: How the backend starts

## What this lesson covers

- How the FastAPI app starts (`main.py`)
- How settings are loaded (`config.py`)
- How the project swaps embedding providers (`embeddings.py`)
- How the project swaps LLM providers (`llm.py`)

## FastAPI entry point

File: `backend/app/main.py`

```python
from fastapi import FastAPI
from app.api.routes import documents, query, evaluation

app = FastAPI(title="ContextIQ API", description="RAG-powered document intelligence platform")

app.include_router(documents.router)
app.include_router(query.router)
app.include_router(evaluation.router)

@app.get("/api/v1/health")
async def health():
    return {"status": "ok", "version": "0.1.0", "model": settings.openai_model}
```

What this means:
- ContextIQ is a **FastAPI** server.
- It has three route groups: documents, query, and evaluation.
- `/api/v1/health` returns a simple health check.

FastAPI is a Python web framework. Think of it as the front door of your backend: it receives HTTP requests, routes them to the right function, and returns JSON responses.

## Configuration

File: `backend/app/core/config.py`

```python
class Settings(BaseSettings):
    embedding_provider: str = "fastembed"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    embedding_model: str = "text-embedding-3-small"
    fastembed_model: str = "BAAI/bge-small-en-v1.5"  # 384 dims
    cohere_api_key: str
    qdrant_url: str = ":memory:"
    llm_provider: str = "groq"
    groq_api_key: str = ""
    groq_model: str = "llama-3.3-70b-versatile"
```

What this does:
- Reads settings from a `.env` file.
- Defaults to **fastembed** for embeddings (local, free, offline).
- Defaults to **groq** for LLM (free tier, Llama 3.3 70B).
- Stores keys for OpenAI, Cohere, Groq, and Qdrant.

### Why fastembed and groq as defaults?

Because your OpenAI key had no billing credits when you built the project.

- `fastembed` runs locally using ONNX (no API calls, no cost).
- `groq` gives free access to a strong Llama 3.3 70B model.
- Both can be swapped to OpenAI by changing one environment variable.

This is a real engineering decision: you abstracted the provider so you could develop without billing while keeping production-grade providers available.

## Embedding factory

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

What this does:
- Returns an embedding model based on the provider setting.
- `fastembed` → local ONNX model, 384-dimensional vectors.
- `openai` → `text-embedding-3-small`, 1536-dimensional vectors.

**Important dimension consequence:** the Qdrant collection dimension must match the embedding provider. If you create a collection with 384-dim vectors and then switch to OpenAI (1536-dim), the code will fail. Your `config.py` handles this with the `embedding_dim` property.

### Why use a factory?

The rest of the code does not need to know which provider is being used. It calls `get_embed_model()` and gets a model with a consistent interface. This is the **strategy pattern** — the provider is swappable.

## LLM factory

File: `backend/app/core/llm.py`

```python
def get_llm():
    provider = settings.llm_provider

    if provider == "groq":
        from llama_index.llms.groq import Groq
        return Groq(model=settings.groq_model, api_key=settings.groq_api_key)

    elif provider == "openai":
        from llama_index.llms.openai import OpenAI
        return OpenAI(model=settings.openai_model)
```

Same pattern as the embedding factory. The code that generates answers does not care whether the underlying model is Groq or OpenAI.

## Why this matters in an interview

You can say:

> "I abstracted both the embedding provider and the LLM provider behind factory functions. The default is fastembed for embeddings and Groq for the LLM because those are free for development, but the code can switch to OpenAI by changing one `.env` variable. This keeps the project cheap to develop while remaining production-ready."

## Common trap

**"Why didn't you just use OpenAI for everything?"**

Strong answer: your OpenAI key had no billing credits, and you wanted to keep development free. You did not let that block you — you built a provider abstraction so you could swap providers later without rewriting the retrieval pipeline.

## Self-check

1. What does FastAPI do in this project?
2. Why is `fastembed` the default embedding provider?
3. Why is `groq` the default LLM provider?
4. What happens if you switch embedding providers without changing the Qdrant collection?
5. What design pattern do `embeddings.py` and `llm.py` use?

## Code map

| Concept | File |
|---|---|
| FastAPI app | `backend/app/main.py` |
| Settings | `backend/app/core/config.py` |
| Embedding factory | `backend/app/core/embeddings.py` |
| LLM factory | `backend/app/core/llm.py` |
