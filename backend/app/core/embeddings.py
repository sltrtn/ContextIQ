"""Embedding model factory — returns the right embed model based on config.

Supports:
  - "fastembed": local ONNX (BAAI/bge-small-en-v1.5, 384 dims) — zero API cost
  - "openai":    OpenAI text-embedding-3-small (1536 dims) — requires billing
"""

from app.core.config import get_settings

settings = get_settings()


def get_embed_model():
    """Return a LlamaIndex-compatible embedding model per config."""
    provider = settings.embedding_provider

    if provider == "fastembed":
        from llama_index.embeddings.fastembed import FastEmbedEmbedding
        return FastEmbedEmbedding(
            model_name=settings.fastembed_model,
        )

    elif provider == "openai":
        from llama_index.embeddings.openai import OpenAIEmbedding
        return OpenAIEmbedding(model=settings.embedding_model)

    else:
        raise ValueError(
            f"Unknown embedding_provider '{provider}'. "
            "Set to 'fastembed' or 'openai' in .env"
        )
