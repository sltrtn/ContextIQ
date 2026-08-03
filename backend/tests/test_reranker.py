"""Unit tests for the reranker and fallback."""

from unittest.mock import patch

from app.retrieval.reranker import rerank


def test_reranker_no_cohere_returns_top_k_input_order():
    docs = [
        {"text": "A", "score": 0.1},
        {"text": "B", "score": 0.9},
        {"text": "C", "score": 0.5},
    ]
    # Patch Cohere client creation to fail, so fallback is used
    with patch("app.retrieval.reranker.cohere.Client", side_effect=Exception("no key")):
        result = rerank("query", docs, top_k=2)
    assert len(result) == 2
    assert result[0]["text"] == "A"
    assert result[1]["text"] == "B"


def test_reranker_with_empty_docs():
    result = rerank("query", [], top_k=5)
    assert result == []


def test_reranker_top_k_larger_than_docs():
    docs = [{"text": "only", "score": 0.5}]
    with patch("app.retrieval.reranker.cohere.Client", side_effect=Exception("no key")):
        result = rerank("query", docs, top_k=5)
    assert len(result) == 1
    assert result[0]["text"] == "only"
