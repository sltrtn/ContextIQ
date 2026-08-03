"""Unit tests for document parsing."""

import os
from pathlib import Path

from app.ingestion.parser import parse_document, parse_document_pages

DATA_DIR = Path(__file__).resolve().parent.parent.parent.parent / "data" / "papers"


def test_parse_document_returns_string():
    files = list(DATA_DIR.glob("*.pdf"))
    if not files:
        return
    text = parse_document(files[0])
    assert isinstance(text, str)
    assert len(text) > 0


def test_parse_document_pages_returns_list_of_dicts():
    files = list(DATA_DIR.glob("*.pdf"))
    if not files:
        return
    pages = parse_document_pages(files[0])
    assert isinstance(pages, list)
    assert len(pages) > 0
    assert isinstance(pages[0], dict)
    assert "text" in pages[0]
    assert "page_number" in pages[0]
    assert pages[0]["page_number"] == 1


def test_parse_document_txt():
    path = Path("/tmp/test_contextiq.txt")
    path.write_text("Hello world.\nSecond line.")
    try:
        text = parse_document(path)
        assert "Hello world" in text
    finally:
        path.unlink()


def test_parse_document_pages_for_pdf():
    files = list(DATA_DIR.glob("*.pdf"))
    if not files:
        return
    pages = parse_document_pages(files[0])
    page_numbers = [p["page_number"] for p in pages]
    assert page_numbers == list(range(1, len(pages) + 1))
