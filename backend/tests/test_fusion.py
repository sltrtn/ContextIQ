"""Unit tests for RRF fusion."""

from app.retrieval.fusion import reciprocal_rank_fusion


def test_rrf_empty_lists():
    result = reciprocal_rank_fusion([], [], top_k=5)
    assert result == []


def test_rrf_single_source():
    dense = [
        {"text": "A", "chunk_id": "0"},
        {"text": "B", "chunk_id": "1"},
    ]
    result = reciprocal_rank_fusion(dense, [], top_k=5)
    assert len(result) == 2
    assert result[0]["chunk_id"] == "0"


def test_rrf_fuses_both_sources():
    dense = [
        {"text": "A", "chunk_id": "0"},
        {"text": "B", "chunk_id": "1"},
        {"text": "C", "chunk_id": "2"},
    ]
    sparse = [
        {"text": "B", "chunk_id": "1"},
        {"text": "A", "chunk_id": "0"},
        {"text": "D", "chunk_id": "3"},
    ]
    result = reciprocal_rank_fusion(dense, sparse, top_k=4)
    ids = [r["chunk_id"] for r in result]
    # A and B appear in both lists, should outrank C and D
    assert ids[0] in ("0", "1")
    assert ids[1] in ("0", "1")
    assert set(ids) == {"0", "1", "2", "3"}


def test_rrf_preserves_text_and_metadata():
    dense = [{"text": "hello", "chunk_id": "c1", "filename": "f.pdf"}]
    sparse = [{"text": "hello", "chunk_id": "c1"}]
    result = reciprocal_rank_fusion(dense, sparse, top_k=1)
    assert result[0]["text"] == "hello"
    assert result[0]["chunk_id"] == "c1"
    assert result[0]["filename"] == "f.pdf"
    assert "score" in result[0]
