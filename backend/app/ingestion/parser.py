from pathlib import Path


def parse_document(file_path: str | Path) -> str:
    """Parse a document (PDF/DOCX/TXT) into plain text.

    Uses pypdf for PDFs (fast, no external deps). Uses python-docx for DOCX.
    Falls back to unstructured if available.
    """
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
