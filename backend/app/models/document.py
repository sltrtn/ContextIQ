from pydantic import BaseModel
from datetime import datetime


class UploadResponse(BaseModel):
    task_id: str
    filename: str
    status: str = "pending"


class DocumentStatus(BaseModel):
    task_id: str
    status: str  # pending / processing / completed / failed
    doc_id: str | None = None
    chunks: int | None = None
    error: str | None = None


class DocumentListItem(BaseModel):
    id: str
    filename: str
    status: str
    uploaded_at: datetime
    chunks: int | None = None
