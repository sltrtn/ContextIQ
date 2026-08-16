from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.core.config import get_settings
from app.api.routes import documents, query, evaluation

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    print(f"ContextIQ API starting — env: {settings.active_llm_model}")
    yield


app = FastAPI(
    title="ContextIQ API",
    description="RAG-powered document intelligence platform",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(documents.router)
app.include_router(query.router)
app.include_router(evaluation.router)


@app.get("/api/v1/health")
async def health():
    return {
        "status": "ok",
        "version": "0.1.0",
        "model": settings.active_llm_model,
    }
