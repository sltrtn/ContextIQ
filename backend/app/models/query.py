from pydantic import BaseModel


class QueryRequest(BaseModel):
    question: str
    document_ids: list[str] | None = None
    top_k: int = 5


class Source(BaseModel):
    text: str
    score: float
    doc_id: str | None = None
    chunk_id: str | None = None


class QueryResponse(BaseModel):
    answer: str
    sources: list[Source]
    metadata: dict = {}
