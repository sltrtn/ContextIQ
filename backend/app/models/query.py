from pydantic import BaseModel


class QueryRequest(BaseModel):
    question: str
    document_ids: list[str] | None = None
    top_k: int = 5
    config: str | None = None  # vector_only, vector_rerank, hybrid, hybrid_rerank, long_context
    expand: bool = False  # Enable query expansion via LLM


class Source(BaseModel):
    text: str
    score: float
    doc_id: str | None = None
    chunk_id: str | None = None
    filename: str | None = None
    page: int | None = None


class FaithfulnessCheck(BaseModel):
    score: float  # 0.0 - 1.0
    supported_claims: int
    total_claims: int
    unsupported_claims: list[str] = []


class QueryResponse(BaseModel):
    answer: str
    sources: list[Source]
    metadata: dict = {}
    faithfulness: FaithfulnessCheck | None = None
