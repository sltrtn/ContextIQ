"""Unit tests for retrieval metrics."""

from app.evaluation.retrieval_metrics import compute_metrics, is_relevant


def test_is_relevant_matches_filename():
    assert is_relevant({"filename": "x.pdf"}, "x.pdf") is True
    assert is_relevant({"filename": "x.pdf"}, "y.pdf") is False
    assert is_relevant({"filename": None}, "x.pdf") is False


def test_compute_metrics_perfect_top_k():
    results = [
        {"text": "a", "filename": "target.pdf"},
        {"text": "b", "filename": "target.pdf"},
        {"text": "c", "filename": "target.pdf"},
    ]
    # Corpus has 5 relevant chunks total
    metrics = compute_metrics(results, "target.pdf", k=3, total_relevant_in_corpus=5)
    assert metrics["precision_at_k"] == 1.0
    assert metrics["recall_at_k"] == 3 / 5
    assert metrics["mrr"] == 1.0


def test_compute_metrics_no_relevant():
    results = [
        {"text": "a", "filename": "other.pdf"},
        {"text": "b", "filename": "other.pdf"},
    ]
    metrics = compute_metrics(results, "target.pdf", k=2, total_relevant_in_corpus=4)
    assert metrics["precision_at_k"] == 0.0
    assert metrics["recall_at_k"] == 0.0
    assert metrics["mrr"] == 0.0


def test_compute_metrics_mrr_second_rank():
    results = [
        {"text": "a", "filename": "other.pdf"},
        {"text": "b", "filename": "target.pdf"},
    ]
    metrics = compute_metrics(results, "target.pdf", k=2, total_relevant_in_corpus=4)
    assert metrics["precision_at_k"] == 0.5
    assert metrics["recall_at_k"] == 1 / 4
    assert metrics["mrr"] == 0.5


def test_compute_metrics_k_zero():
    results = [{"text": "a", "filename": "target.pdf"}]
    metrics = compute_metrics(results, "target.pdf", k=0, total_relevant_in_corpus=4)
    assert metrics["precision_at_k"] == 0.0
    assert metrics["recall_at_k"] == 0.0
    assert metrics["mrr"] == 1.0


def test_compute_metrics_fallback_total():
    # No total_relevant_in_corpus passed: function computes from results
    results = [
        {"text": "a", "filename": "target.pdf"},
        {"text": "b", "filename": "target.pdf"},
    ]
    metrics = compute_metrics(results, "target.pdf", k=2)
    assert metrics["recall_at_k"] == 1.0
    assert metrics["total_relevant"] == 2
