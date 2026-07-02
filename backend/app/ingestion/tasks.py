import os
import uuid
from pathlib import Path

from celery import Celery
from llama_index.core import Document as LlamaDocument
from llama_index.core.node_parser import SentenceSplitter
from llama_index.embeddings.openai import OpenAIEmbedding
from llama_index.vector_stores.qdrant import QdrantVectorStore
from llama_index.core import StorageContext, VectorStoreIndex
from qdrant_client import QdrantClient

from app.core.config import get_settings
from app.ingestion.parser import parse_document
from app.ingestion.chunker import chunk_document
from app.retrieval.dense import get_qdrant_client, ensure_collection

settings = get_settings()

celery_app = Celery(
    "contextiq",
    broker=settings.celery_broker_url,
    backend=settings.celery_result_backend,
)

# In-memory tracking of ingest status (for dev without Redis persistence)
_ingestion_status: dict[str, str] = {}


@celery_app.task(bind=True, max_retries=3)
def ingest_document(self, file_path: str, doc_id: str | None = None) -> dict:
    """Parse, chunk, embed, and index a document into Qdrant."""
    task_id = self.request.id
    _ingestion_status[task_id] = "processing"

    try:
        if doc_id is None:
            doc_id = str(uuid.uuid4())

        # Parse
        text = parse_document(file_path)
        if not text.strip():
            raise ValueError("Document contains no extractable text")

        # Chunk
        chunks = chunk_document(text, strategy="sentence_window")
        if not chunks:
            raise ValueError("No chunks generated from document")

        # Embed and index via LlamaIndex + Qdrant
        qdrant_client = get_qdrant_client()
        ensure_collection(qdrant_client)

        vector_store = QdrantVectorStore(
            client=qdrant_client,
            collection_name=settings.qdrant_collection,
        )

        embed_model = OpenAIEmbedding(model=settings.embedding_model)
        documents = [
            LlamaDocument(
                text=chunk["text"],
                metadata={"doc_id": doc_id, "chunk_id": chunk["node_id"]},
            )
            for chunk in chunks
        ]

        StorageContext.from_defaults(vector_store=vector_store)
        VectorStoreIndex.from_documents(
            documents,
            embed_model=embed_model,
            vector_store=vector_store,
            show_progress=True,
        )

        _ingestion_status[task_id] = "completed"
        return {
            "task_id": task_id,
            "doc_id": doc_id,
            "status": "completed",
            "chunks": len(chunks),
            "file": os.path.basename(file_path),
        }

    except Exception as exc:
        _ingestion_status[task_id] = "failed"
        raise self.retry(exc=exc, countdown=60)


def get_status(task_id: str) -> str | None:
    return _ingestion_status.get(task_id)
