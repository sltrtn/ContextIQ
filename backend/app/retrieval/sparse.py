"""BM25 sparse retriever using rank_bm25."""

from rank_bm25 import BM25Okapi
import nltk
from nltk.tokenize import word_tokenize

try:
    nltk.data.find("tokenizers/punkt")
except LookupError:
    nltk.download("punkt_tab", quiet=True)


_global_bm25 = None


class BM25Retriever:
    """BM25 retriever that indexes chunks for retrieval."""

    def __init__(self):
        self._chunks: list[dict] = []
        self._tokenized: list[list[str]] = []
        self._bm25: BM25Okapi | None = None

    def index(self, chunks: list[dict]):
        if not chunks:
            self._chunks = []
            self._tokenized = []
            self._bm25 = None
            return
        self._chunks = chunks
        self._tokenized = [word_tokenize(c["text"].lower()) for c in chunks]
        self._bm25 = BM25Okapi(self._tokenized)

    def retrieve(self, query: str, top_k: int = 20) -> list[dict]:
        if self._bm25 is None:
            return []
        tokenized_query = word_tokenize(query.lower())
        scores = self._bm25.get_scores(tokenized_query)
        scored = list(enumerate(scores))
        scored.sort(key=lambda x: x[1], reverse=True)
        top = scored[:top_k]
        results = []
        for idx, score in top:
            result = dict(self._chunks[idx])
            result["score"] = float(score)
            result["chunk_id"] = self._chunks[idx].get("chunk_id", self._chunks[idx].get("node_id", ""))
            result["index"] = idx
            results.append(result)
        return results


def build_global_bm25(chunks: list[dict]):
    """Build the global BM25 index from all ingested chunks."""
    global _global_bm25
    _global_bm25 = BM25Retriever()
    _global_bm25.index(chunks)


def get_global_bm25() -> BM25Retriever | None:
    """Return the global BM25 index, or None if not built yet."""
    return _global_bm25
