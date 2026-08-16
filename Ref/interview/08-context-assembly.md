# Lesson 8 — Context Assembly

## What this lesson covers

- Deduplication of retrieved chunks
- Lost-in-the-middle ordering
- Source labeling for citations
- How all three combine into the final prompt context

## The three jobs of context assembly

After retrieval, fusion, and reranking, you have a ranked list of chunks. Before sending them to the LLM, your code does three things:

1. **Deduplicate** near-duplicate chunks.
2. **Order** them for best LLM attention.
3. **Label** them so the LLM can cite sources.

File: `backend/app/retrieval/context_assembly.py`

## 1. Deduplication

```python
def _text_similarity(a, b):
    return SequenceMatcher(None, a[:200], b[:200]).ratio()

def deduplicate(chunks, threshold=0.85):
    unique = [chunks[0]]
    for chunk in chunks[1:]:
        is_dup = False
        for existing in unique:
            if _text_similarity(chunk["text"], existing["text"]) > threshold:
                if chunk.get("score", 0) > existing.get("score", 0):
                    unique.remove(existing)
                    unique.append(chunk)
                is_dup = True
                break
        if not is_dup:
            unique.append(chunk)
    return unique
```

What it does:
- Compares the first 200 characters of each pair of chunks using `SequenceMatcher`.
- If similarity > 0.85, it treats them as duplicates.
- Keeps the duplicate with the higher score.

This is a cheap textual heuristic. It will **not** catch paraphrased duplicates or near-meaning rewrites.

## 2. Lost-in-the-middle ordering

```python
def order_chunks(chunks, strategy="reversed"):
    if strategy == "original":
        return chunks
    elif strategy == "forward":
        return list(reversed(chunks))
    elif strategy == "reversed":
        return list(chunks)
```

In your pipeline, chunks are already in rank order (best first). The `reversed` strategy keeps them that way.

Why? Because when the context string is built, chunks are joined together and then the question is appended. The chunk at the **end** of the context string is closest to the question. Research shows LLMs attend better to information near the start and end of context than to the middle.

So by keeping the best chunk at the end, you place the most relevant evidence closest to the question.

## 3. Source labeling

```python
def label_sources(chunks, start_index=1):
    labeled = []
    for i, chunk in enumerate(chunks, start=start_index):
        filename = chunk.get("filename", "unknown")
        page = chunk.get("page_number") or chunk.get("page")
        page_str = f" (p.{page})" if page else ""
        text = chunk["text"]
        labeled.append(f"[{i}] {filename}{page_str}: {text}")
    return labeled
```

This formats chunks like:

```
[1] 2305.18290_DPO.pdf (p.5): DPO uses a classification loss to align language models...
```

The LLM is prompted to cite sources using `[1]`, `[2]`, etc. Because the labels are clear, the generated answer can include citations.

## The assembly pipeline

```python
def assemble_context(chunks, dedup=True, order="reversed", max_chunks=5, with_labels=True):
    if dedup:
        chunks = deduplicate(chunks)
    chunks = order_chunks(chunks, strategy=order)
    chunks = chunks[:max_chunks]
    if with_labels:
        labeled = label_sources(chunks)
    else:
        labeled = [c["text"] for c in chunks]
    return "\n\n".join(labeled)
```

Flow: dedup → order → limit → label → join.

## Why this matters in an interview

You can say:

> "After retrieving the top chunks, we assemble them for the LLM: we deduplicate near-identical chunks using a SequenceMatcher heuristic, order them so the most relevant chunk appears last and closest to the question, and label each with `[1] filename (p.N)` so the LLM can cite its sources."

## Common trap

**"How do you handle duplicate information?"**

Strong answer: a textual similarity heuristic on the first 200 characters, keeping the higher-scored duplicate. Then be honest: it won't catch paraphrased duplicates, which is a known limitation.

## Self-check

1. What are the three jobs of context assembly?
2. How does deduplication work?
3. What is the limitation of the deduplication method?
4. Why is the most relevant chunk placed last in the context string?
5. What does a source label look like?
6. What is the default `max_chunks` value?

## Code map

| Concept | File |
|---|---|
| Deduplication | `backend/app/retrieval/context_assembly.py` |
| Ordering | `backend/app/retrieval/context_assembly.py` |
| Source labeling | `backend/app/retrieval/context_assembly.py` |
| Full assembly | `backend/app/retrieval/context_assembly.py` |
