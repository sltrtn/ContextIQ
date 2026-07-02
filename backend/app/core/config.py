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

    # OpenAI
    openai_api_key: str
    openai_model: str = "gpt-4o-mini"
    embedding_model: str = "text-embedding-3-small"

    # Cohere
    cohere_api_key: str

    # Qdrant (set url=":memory:" for in-memory dev, no Docker needed)
    qdrant_url: str = ":memory:"
    qdrant_api_key: str | None = None
    qdrant_collection: str = "contextiq_docs"

    # Celery / Redis
    celery_broker_url: str = "redis://localhost:6379/0"
    celery_result_backend: str = "redis://localhost:6379/0"

    # API Security
    api_key: str | None = None  # optional static API key for backend auth


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    # Export to env so LlamaIndex, OpenAI, Cohere SDKs can find them
    os.environ.setdefault("OPENAI_API_KEY", settings.openai_api_key)
    os.environ.setdefault("COHERE_API_KEY", settings.cohere_api_key)
    return settings
