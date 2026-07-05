from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache


import os
from pathlib import Path

_env_path = Path(__file__).resolve().parent.parent.parent / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(_env_path),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # Embedding provider: "openai" or "fastembed" (local, no API cost)
    embedding_provider: str = "fastembed"  # switch to "openai" when billing added

    # OpenAI
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    embedding_model: str = "text-embedding-3-small"

    # FastEmbed (local, no API cost)
    fastembed_model: str = "BAAI/bge-small-en-v1.5"  # 384 dims

    @property
    def embedding_dim(self) -> int:
        """Vector dimension for Qdrant collection."""
        if self.embedding_provider == "fastembed":
            return 384
        return 1536  # OpenAI text-embedding-3-small

    # Cohere
    cohere_api_key: str

    # Qdrant (set url=":memory:" for in-memory dev, no Docker needed)
    qdrant_url: str = ":memory:"
    qdrant_api_key: str | None = None
    qdrant_collection: str = "contextiq_docs"

    # LLM provider: "openai" or "groq" (free, no billing)
    llm_provider: str = "groq"  # switch to "openai" when billing added
    groq_api_key: str = ""
    groq_model: str = "llama-3.3-70b-versatile"  # free fast Llama 3.3 70B

    # Celery / Redis
    celery_broker_url: str = "redis://localhost:6379/0"
    celery_result_backend: str = "redis://localhost:6379/0"

    # API Security
    api_key: str | None = None  # optional static API key for backend auth


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    # Export to env so LlamaIndex, OpenAI, Cohere SDKs can find them
    if settings.openai_api_key:
        os.environ.setdefault("OPENAI_API_KEY", settings.openai_api_key)
    os.environ.setdefault("COHERE_API_KEY", settings.cohere_api_key)
    return settings
