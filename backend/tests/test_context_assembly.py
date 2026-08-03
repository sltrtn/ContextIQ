"""Unit tests for context assembly utilities."""

from app.retrieval.context_assembly import (
    deduplicate,
    order_chunks,
    label_sources,
    assemble_context,
)


def test_deduplicate_removes_near_duplicates():
    chunks = [
        {"text": "This is exactly the same text repeated again." * 2, "score": 0.9, "filename": "a.pdf", "page_number": 1},
        {"text": "This is exactly the same text repeated again." * 2, "score": 0.8, "filename": "a.pdf", "page_number": 1},
        {"text": "Different content here for testing purposes.", "score": 0.7, "filename": "b.pdf", "page_number": 2},
    ]
    result = deduplicate(chunks, threshold=0.85)
    assert len(result) == 2
    # Higher score kept
    assert result[0]["score"] == 0.9


def test_deduplicate_keeps_different_text():
    chunks = [
        {"text": "Alpha beta gamma.", "score": 0.9},
        {"text": "Delta epsilon zeta.", "score": 0.8},
    ]
    result = deduplicate(chunks, threshold=0.85)
    assert len(result) == 2


def test_order_chunks_forward():
    chunks = [{"text": "A", "score": 0.3}, {"text": "B", "score": 0.9}]
    result = order_chunks(chunks, strategy="forward")
    assert result[0]["text"] == "B"


def test_order_chunks_reversed():
    chunks = [{"text": "A", "score": 0.3}, {"text": "B", "score": 0.9}]
    result = order_chunks(chunks, strategy="reversed")
    assert result[0]["text"] == "A"


def test_label_sources_with_page():
    chunks = [
        {"text": "hello", "filename": "x.pdf", "page_number": 5},
        {"text": "world", "filename": "y.pdf", "page_number": None},
    ]
    labels = label_sources(chunks)
    assert labels[0].startswith("[1] x.pdf (p.5):")
    assert labels[1].startswith("[2] y.pdf:")


def test_assemble_context_full_pipeline():
    chunks = [
        {"text": "First chunk." * 10, "score": 0.9, "filename": "a.pdf", "page_number": 1},
        {"text": "Second chunk." * 10, "score": 0.8, "filename": "b.pdf", "page_number": 2},
    ]
    context = assemble_context(chunks, max_chunks=2)
    assert "[1] a.pdf (p.1):" in context
    assert "[2] b.pdf (p.2):" in context
    assert "First chunk" in context
    assert "Second chunk" in context


def test_assemble_context_dedups():
    chunks = [
        {"text": "Duplicate text here for testing." * 2, "score": 0.9, "filename": "a.pdf", "page_number": 1},
        {"text": "Duplicate text here for testing." * 2, "score": 0.8, "filename": "b.pdf", "page_number": 2},
    ]
    context = assemble_context(chunks, max_chunks=5)
    # Should deduplicate to one chunk (only one source label)
    assert context.count("[1] ") == 1
    assert "[2]" not in context
