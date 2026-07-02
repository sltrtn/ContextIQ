"""BM25 sparse retriever using rank_bm25."""

from rank_bm25 import BM25Okapi
import nltk
from nltk.tokenize import word_tokenize

try:
    nltk.data.find("tokenizers/punkt")
except LookupError:
    nltk.download("punkt_tab", quiet=True)


class BM25Retriever:
    """Simple BM25 retriever that indexes chunks for a single document."""

    def __init__(self):
        self._chunks: list[dict] = []
        self._tokenized: list[list[str]] = []
        self._bm25: BM25Okapi | None = None

    def index(self, chunks: list[dict]):
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
        return [
            {
                "text": self._chunks[idx]["text"],
                "score": float(score),
                "chunk_id": self._chunks[idx].get("node_id", ""),
                "index": idx,
            }
            for idx, score in top
        ]
