from qdrant_client import QdrantClient

from app.core.config import get_settings

settings = get_settings()

_client: QdrantClient | None = None


def get_qdrant_client() -> QdrantClient:
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


def ensure_collection(client: QdrantClient | None = None) -> QdrantClient:
    """Ensure the Qdrant collection exists and return the client."""
    if client is None:
        client = get_qdrant_client()
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
    return client
