"""Unit tests for chunking strategies."""

from app.ingestion.chunker import (
    sentence_window_chunker,
    semantic_chunker,
    contextual_chunker,
    chunk_pages,
    _detect_sections,
    _summarize_sections,
)


class FakeLLM:
    def __init__(self, response):
        self.response = response

    def complete(self, prompt):
        return self.response


def test_sentence_window_chunker():
    text = "This is sentence one. " * 50 + "This is sentence two. " * 50
    chunks = sentence_window_chunker(text, chunk_size=128, chunk_overlap=20)
    assert len(chunks) > 0
    assert all("text" in c for c in chunks)
    # Joined chunks should cover all input text
    joined = " ".join(c["text"] for c in chunks)
    assert "sentence one" in joined
    assert "sentence two" in joined


def test_detect_sections_finds_headers():
    text = """Abstract
This is the abstract.

Introduction
This is the intro.

Method
We did things.

Results
The results are good.
"""
    sections = _detect_sections(text)
    names = [s[0] for s in sections]
    assert "Abstract" in names
    assert "Introduction" in names
    assert "Method" in names
    assert "Results" in names


def test_summarize_sections_with_mock_llm():
    sections = [("Abstract", "We introduce a method."), ("Method", "We use neural networks.")]
    llm = FakeLLM("[1] Introduces the method.\n[2] Describes neural networks.")
    summaries = _summarize_sections(sections, llm=llm)
    assert summaries["Abstract"] == "Introduces the method."
    assert summaries["Method"] == "Describes neural networks."


def test_summarize_sections_single_section():
    sections = [("Abstract", "We introduce a method.")]
    summaries = _summarize_sections(sections)
    assert summaries["Abstract"] == ""


def test_contextual_chunker_with_mock_llm():
    pages = [
        {"text": "Abstract\nThis paper studies chunking.\n\nIntroduction\nWe need better chunks.", "page_number": 1}
    ]
    llm = FakeLLM("[1] Summary of abstract.\n[2] Summary of intro.")
    chunks = contextual_chunker(pages, chunk_size=64, chunk_overlap=10, llm=llm)
    assert len(chunks) > 0
    # All chunks should have a section label
    for c in chunks:
        assert "[Section:" in c["text"]
        assert "page_number" in c
        assert c["page_number"] == 1


def test_chunk_pages_dispatcher():
    pages = [
        {"text": "First page. " * 50, "page_number": 1},
        {"text": "Second page. " * 50, "page_number": 2},
    ]
    chunks = chunk_pages(pages, strategy="sentence_window", chunk_size=128)
    assert len(chunks) > 0
    assert all("page_number" in c for c in chunks)
    page_numbers = {c["page_number"] for c in chunks}
    assert page_numbers == {1, 2}


def test_chunk_pages_contextual_dispatcher():
    pages = [
        {"text": "Abstract\nThis paper studies chunking.", "page_number": 1},
    ]
    llm = FakeLLM("[1] Summary of abstract.")
    chunks = chunk_pages(pages, strategy="contextual", chunk_size=64, llm=llm)
    assert len(chunks) > 0
    assert all("[Section:" in c["text"] for c in chunks)
