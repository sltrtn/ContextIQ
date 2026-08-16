# Lesson 3 — Ingest and Parse: Uploading a document

## What this lesson covers

- The upload endpoint
- How documents are parsed into pages
- Why page metadata matters
- How chunks are created and indexed
- How the BM25 index is rebuilt

## The upload endpoint

File: `backend/app/api/routes/documents.py`

```python
@router.post("/upload", response_model=UploadResponse)
async def upload_document(file: UploadFile = File(...)):
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in (".pdf", ".docx", ".txt"):
        raise HTTPException(400, f"Unsupported file type: {ext}")

    doc_id = str(uuid.uuid4())
    safe_name = f"{doc_id}{ext}"
    file_path = UPLOAD_DIR / safe_name

    content = await file.read()
    file_path.write_bytes(content)

    result = _ingest_sync(str(file_path), doc_id, filename=file.filename)
    return UploadResponse(task_id=doc_id, filename=file.filename, status=result["status"])
```

What happens:
1. User uploads a file via HTTP POST to `/api/v1/documents/upload`.
2. The backend checks the extension (PDF, DOCX, TXT only).
3. It assigns a UUID, saves the file to `data/uploads/`, and calls `_ingest_sync`.

## The ingestion pipeline

File: `backend/app/api/routes/documents.py`, function `_ingest_sync`

```python
def _ingest_sync(file_path, doc_id, filename):
    pages = parse_document_pages(file_path)
    chunks = chunk_pages(pages, strategy="contextual")
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
            metadata={
                "doc_id": doc_id,
                "chunk_id": chunk["node_id"],
                "filename": filename,
                "page_number": chunk.get("page_number"),
            },
        )
        for chunk in chunks
    ]

    VectorStoreIndex.from_documents(
        documents,
        storage_context=storage_context,
        embed_model=embed_model,
        show_progress=True,
    )

    # Update global BM25 index
    global _all_chunks
    _all_chunks.extend(chunks_with_ids)
    build_global_bm25(_all_chunks)
```

Ingest flow:
1. **Parse** the file into pages with text.
2. **Chunk** the pages using the contextual chunker.
3. **Ensure** the Qdrant collection exists.
4. **Embed** each chunk and store it in Qdrant with metadata.
5. **Rebuild** the global BM25 index so sparse retrieval sees the new document.

## The parser

File: `backend/app/ingestion/parser.py`

```python
def parse_document_pages(file_path):
    ext = file_path.suffix.lower()
    if ext == ".pdf":
        return _parse_pdf_pages(file_path)
    text = parse_document(file_path)
    return [{"text": text, "page_number": 1}]


def _parse_pdf_pages(file_path):
    reader = pypdf.PdfReader(str(file_path))
    pages = []
    for i, page in enumerate(reader.pages, start=1):
        text = page.extract_text()
        if text:
            pages.append({"text": text, "page_number": i})
    return pages
```

What it does:
- PDFs are parsed page by page using `pypdf`.
- Each page gets a `page_number`.
- DOCX and TXT files are treated as one page.

### Why page numbers matter

Every chunk is tagged with its source filename and page number. Later, when the system answers a question, it can cite the chunk like:

> According to [1] `2305.18290_DPO.pdf` (p.5) ...

Page metadata is what makes the answer **citable and verifiable**.

### Honest limitation

`pypdf` extracts text but does not understand layout.

- Multi-column PDFs may extract text in the wrong order.
- Figures, tables, and equations are not handled well.

This is a legitimate next-step improvement: you could mention layout-aware parsers like `pymupdf` or OCR for scanned PDFs.

## The BM25 rebuild

After embedding, every chunk is also added to the global BM25 index:

```python
global _all_chunks
_all_chunks.extend(chunks_with_ids)
build_global_bm25(_all_chunks)
```

This is critical. BM25 is a keyword-based search index. If you do not rebuild it after each upload, queries will not find chunks from the new document.

## Why this matters in an interview

You can say:

> "When a user uploads a document, the backend parses it page by page, chunks it with contextual section labels, embeds each chunk into Qdrant, and rebuilds the global BM25 index. Page numbers are preserved so the final answer can cite specific pages."

## Common trap

**"How do you handle figures and tables in PDFs?"**

Strong answer: the parser is text-extraction only. It does not handle layout, figures, or tables well. A production upgrade would use a layout-aware parser or OCR pipeline.

## Self-check

1. What file types are accepted?
2. Why does the parser preserve page numbers?
3. What happens after the chunks are created?
4. Why must the BM25 index be rebuilt after every upload?
5. What is an honest limitation of `pypdf` in this project?

## Code map

| Concept | File |
|---|---|
| Upload endpoint | `backend/app/api/routes/documents.py` |
| PDF parsing | `backend/app/ingestion/parser.py` |
| Page metadata | `backend/app/ingestion/parser.py` |
| Chunking | `backend/app/ingestion/chunker.py` |
| Global BM25 build | `backend/app/retrieval/sparse.py` |
