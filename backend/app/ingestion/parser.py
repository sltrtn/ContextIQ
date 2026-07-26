from pathlib import Path


def parse_document(file_path: str | Path) -> str:
    """Parse a document (PDF/DOCX/TXT) into plain text."""
    file_path = Path(file_path)
    if not file_path.exists():
        raise FileNotFoundError(f"File not found: {file_path}")

    ext = file_path.suffix.lower()

    if ext == ".txt":
        return file_path.read_text(encoding="utf-8", errors="replace")

    if ext == ".pdf":
        return _parse_pdf(file_path)

    if ext == ".docx":
        return _parse_docx(file_path)

    raise ValueError(f"Unsupported file type: {ext}")


def parse_document_pages(file_path: str | Path) -> list[dict]:
    """Parse a document and return list of {text, page_number} dicts."""
    file_path = Path(file_path)
    if not file_path.exists():
        raise FileNotFoundError(f"File not found: {file_path}")

    ext = file_path.suffix.lower()

    if ext == ".pdf":
        return _parse_pdf_pages(file_path)

    # For non-PDF, return single page
    text = parse_document(file_path)
    return [{"text": text, "page_number": 1}]


def _parse_pdf(file_path: Path) -> str:
    try:
        import pypdf
    except ImportError:
        raise ImportError("Install pypdf: pip install pypdf")

    reader = pypdf.PdfReader(str(file_path))
    pages = []
    for page in reader.pages:
        text = page.extract_text()
        if text:
            pages.append(text)
    return "\n\n".join(pages)


def _parse_pdf_pages(file_path: Path) -> list[dict]:
    try:
        import pypdf
    except ImportError:
        raise ImportError("Install pypdf: pip install pypdf")

    reader = pypdf.PdfReader(str(file_path))
    pages = []
    for i, page in enumerate(reader.pages, start=1):
        text = page.extract_text()
        if text:
            pages.append({"text": text, "page_number": i})
    return pages


def _parse_docx(file_path: Path) -> str:
    try:
        from docx import Document
    except ImportError:
        raise ImportError("Install python-docx: pip install python-docx")

    doc = Document(str(file_path))
    return "\n\n".join(p.text for p in doc.paragraphs if p.text)


def parse_document_with_metadata(file_path: str | Path) -> tuple[str, dict]:
    """Parse document and return (text, metadata)."""
    file_path = Path(file_path)
    text = parse_document(file_path)
    metadata = {
        "filename": file_path.name,
        "extension": file_path.suffix,
        "size_bytes": file_path.stat().st_size,
    }
    return text, metadata
