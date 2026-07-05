import os
import uuid
from pathlib import Path

from fastapi import APIRouter, UploadFile, File, HTTPException

from app.models.document import UploadResponse, DocumentStatus

router = APIRouter(prefix="/api/v1/documents", tags=["documents"])

UPLOAD_DIR = Path("data/uploads")
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)


def _ingest_sync(file_path: str, doc_id: str) -> dict:
    """Ingest a document synchronously (no Celery/Redis needed)."""
    from app.ingestion.parser import parse_document
    from app.ingestion.chunker import chunk_document
    from llama_index.core import Document as LlamaDocument, VectorStoreIndex, StorageContext
    from llama_index.vector_stores.qdrant import QdrantVectorStore
    from app.retrieval.dense import get_qdrant_client, ensure_collection
    from app.core.config import get_settings
    from app.core.embeddings import get_embed_model

    settings = get_settings()

    # Parse
    text = parse_document(file_path)
    if not text.strip():
        raise ValueError("Document contains no extractable text")

    # Chunk
    chunks = chunk_document(text, strategy="sentence_window")
    if not chunks:
        raise ValueError("No chunks generated")

    # Embed + index
    qdrant_client = get_qdrant_client()
    ensure_collection(qdrant_client)

    vector_store = QdrantVectorStore(
        client=qdrant_client,
        collection_name=settings.qdrant_collection,
    )
    embed_model = get_embed_model()

    documents = [
        LlamaDocument(
            text=chunk["text"],
            metadata={"doc_id": doc_id, "chunk_id": chunk["node_id"]},
        )
        for chunk in chunks
    ]

    VectorStoreIndex.from_documents(
        documents,
        embed_model=embed_model,
        vector_store=vector_store,
        show_progress=True,
    )

    return {
        "doc_id": doc_id,
        "status": "completed",
        "chunks": len(chunks),
        "file": os.path.basename(file_path),
    }


@router.post("/upload", response_model=UploadResponse)
async def upload_document(file: UploadFile = File(...)):
    """Upload a document. Ingests synchronously (async Celery path coming with Docker)."""
    if not file.filename:
        raise HTTPException(400, "No filename provided")

    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in (".pdf", ".docx", ".txt"):
        raise HTTPException(400, f"Unsupported file type: {ext}")

    doc_id = str(uuid.uuid4())
    safe_name = f"{doc_id}{ext}"
    file_path = UPLOAD_DIR / safe_name

    content = await file.read()
    file_path.write_bytes(content)

    try:
        result = _ingest_sync(str(file_path), doc_id)
        return UploadResponse(
            task_id=doc_id,
            filename=file.filename,
            status=result["status"],
        )
    except Exception as e:
        file_path.unlink(missing_ok=True)
        raise HTTPException(500, f"Ingestion failed: {e}")


@router.get("/{doc_id}/status", response_model=DocumentStatus)
async def document_status(doc_id: str):
    """Check document status (always completed for sync ingestion)."""
    return DocumentStatus(
        task_id=doc_id,
        status="completed",
        doc_id=doc_id,
    )
