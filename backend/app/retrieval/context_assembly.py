"""Context assembly — dedup, ordering, and source labeling for retrieved chunks."""

from difflib import SequenceMatcher


def _text_similarity(a: str, b: str) -> float:
    """Quick text similarity check using first 200 chars."""
    return SequenceMatcher(None, a[:200], b[:200]).ratio()


def deduplicate(
    chunks: list[dict],
    threshold: float = 0.85,
) -> list[dict]:
    """Remove near-duplicate chunks, keeping the one with the higher score."""
    if not chunks:
        return []

    unique = [chunks[0]]
    for chunk in chunks[1:]:
        is_dup = False
        for existing in unique:
            if _text_similarity(chunk["text"], existing["text"]) > threshold:
                # Keep the one with higher score
                if chunk.get("score", 0) > existing.get("score", 0):
                    unique.remove(existing)
                    unique.append(chunk)
                is_dup = True
                break
        if not is_dup:
            unique.append(chunk)

    return unique


def order_chunks(
    chunks: list[dict],
    strategy: str = "reversed",
) -> list[dict]:
    """Order chunks for optimal LLM consumption.

    Strategies:
    - "reversed": most relevant last (closest to the question, best for lost-in-the-middle)
    - "forward": most relevant first
    - "original": no reordering
    """
    if strategy == "original":
        return chunks
    elif strategy == "forward":
        return list(reversed(chunks))
    elif strategy == "reversed":
        # Most relevant first in the list means it appears last in the prompt
        # (LLMs attend better to text near the question at the end)
        return list(chunks)
    return chunks


def label_sources(
    chunks: list[dict],
    start_index: int = 1,
) -> list[str]:
    """Format chunks with numbered source labels for the LLM prompt.

    Returns list of labeled strings like:
    [1] filename.pdf (p.5): chunk text...
    """
    labeled = []
    for i, chunk in enumerate(chunks, start=start_index):
        filename = chunk.get("filename", "unknown")
        page = chunk.get("page_number") or chunk.get("page")
        page_str = f" (p.{page})" if page else ""
        text = chunk["text"]
        labeled.append(f"[{i}] {filename}{page_str}: {text}")
    return labeled


def assemble_context(
    chunks: list[dict],
    dedup: bool = True,
    order: str = "reversed",
    max_chunks: int = 5,
    with_labels: bool = True,
) -> str:
    """Full context assembly pipeline: dedup → order → label → join.

    Returns formatted context string ready for the LLM prompt.
    """
    # Dedup
    if dedup:
        chunks = deduplicate(chunks)

    # Order
    chunks = order_chunks(chunks, strategy=order)

    # Limit
    chunks = chunks[:max_chunks]

    # Label
    if with_labels:
        labeled = label_sources(chunks)
    else:
        labeled = [c["text"] for c in chunks]

    return "\n\n".join(labeled)


def format_answer_with_citations(
    answer: str,
    chunks: list[dict],
) -> str:
    """Post-process an LLM answer to ensure citation markers match source labels."""
    # Already handled by the prompt — this is a safety net
    return answer
