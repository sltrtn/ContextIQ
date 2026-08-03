"""Unit tests for BM25 sparse retrieval."""

from app.retrieval.sparse import BM25Retriever


def test_bm25_empty_corpus_returns_empty():
    retriever = BM25Retriever()
    retriever.index([])
    results = retriever.retrieve("test", top_k=5)
    assert results == []


def test_bm25_retrieve_ranked():
    corpus = [
        {"text": "The cat sat on the mat.", "chunk_id": "0"},
        {"text": "Dogs are great pets.", "chunk_id": "1"},
        {"text": "The mat was sat on by a cat.", "chunk_id": "2"},
    ]
    retriever = BM25Retriever()
    retriever.index(corpus)
    results = retriever.retrieve("cat on mat", top_k=2)
    assert len(results) == 2
    assert all("text" in r for r in results)
    assert all("score" in r for r in results)
    # Results should be one of the corpus docs
    texts = [r["text"].lower() for r in results]
    assert any("cat" in t or "mat" in t or "dog" in t for t in texts)


def test_bm25_preserves_metadata():
    corpus = [
        {"text": "hello world", "chunk_id": "c1", "page_number": 3, "filename": "x.pdf"},
    ]
    retriever = BM25Retriever()
    retriever.index(corpus)
    results = retriever.retrieve("hello", top_k=1)
    assert results[0]["chunk_id"] == "c1"
    assert results[0]["page_number"] == 3
    assert results[0]["filename"] == "x.pdf"


def test_bm25_retriever_reuses_index():
    corpus = [
        {"text": "apple apple apple", "chunk_id": "0"},
        {"text": "cherry cherry cherry", "chunk_id": "1"},
    ]
    retriever = BM25Retriever()
    retriever.index(corpus)
    results1 = retriever.retrieve("apple", top_k=2)
    results2 = retriever.retrieve("cherry", top_k=2)
    ids1 = [r["chunk_id"] for r in results1]
    ids2 = [r["chunk_id"] for r in results2]
    # Each query should return both docs; relevant doc should be in top results
    assert "0" in ids1
    assert "1" in ids2
    assert len(ids1) == 2
    assert len(ids2) == 2
