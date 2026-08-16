# Lesson 4 — Chunking: Why this is your most defensible piece

## What this lesson covers

- Why chunking is needed
- Three chunking strategies in your code
- Contextual chunking: section detection, summarization, and prepending labels
- The honest caveat about this technique

## Why chunking matters

A research paper might be 10,000 tokens. An LLM cannot process all of that at once efficiently. Also, even if it could, mixing relevant and irrelevant text makes the answer worse.

Chunking splits the document into smaller pieces. The retriever searches over these pieces instead of the whole document.

Your chunking is not just mechanical — it is **semantic**. The contextual chunker prepends a section summary to each chunk so the chunk does not lose its place in the document.

## The three strategies

File: `backend/app/ingestion/chunker.py`

### 1. Sentence-window chunker

```python
def sentence_window_chunker(text, chunk_size=512, chunk_overlap=50):
    splitter = SentenceSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
    doc = LlamaDocument(text=text)
    nodes = splitter.get_nodes_from_documents([doc])
    return [{"text": node.text, "node_id": node.node_id} for node in nodes]
```

What it does:
- Splits text into fixed-size chunks of roughly 512 tokens.
- Each chunk overlaps the previous one by 50 tokens.
- This is the simplest, most common strategy.

Limitation: a chunk like "we set λ = 0.1" is meaningless without knowing which section it came from.

### 2. Semantic chunker

```python
def semantic_chunker(text, buffer_size=1, breakpoint_percentile_threshold=95):
    embed_model = OpenAIEmbedding(model="text-embedding-3-small")
    splitter = SemanticSplitterNodeParser(...)
    nodes = splitter.get_nodes_from_documents([doc])
```

What it does:
- Splits the document at points where the meaning between consecutive sentences changes significantly.
- Uses embedding distances to detect topic boundaries.

This is less used in your default pipeline because it requires an OpenAI embedding call per chunk during ingestion. Your default is contextual chunking.

### 3. Contextual chunker (your default)

```python
def contextual_chunker(pages, chunk_size=512, chunk_overlap=50, llm=None):
    full_text = '\n\n'.join(p["text"] for p in pages)
    sections = _detect_sections(full_text)
    summaries = _summarize_sections(sections, llm)

    for page in pages:
        for sec_name, sec_text in page_sections:
            chunks = sentence_window_chunker(sec_text, ...)
            summary = summaries.get(sec_name, "")
            section_label = f"[Section: {sec_name} — {summary}]"
            for c in chunks:
                c["text"] = f"{section_label}\n\n{c['text']}"
```

What it does:
1. Detects section headers (Abstract, Introduction, Method, Results, etc.) using a regex.
2. Asks the LLM to summarize each section in one sentence (max 15 words) — **one batched call for the whole document**.
3. Prepend `[Section: Method — This section describes the QLoRA technique...]` to every chunk in that section.

Example chunk before:

> "we set λ = 0.1 and freeze the base model weights"

Example chunk after:

> "[Section: Method — This section describes the QLoRA fine-tuning technique using 4-bit quantization.]\n\nwe set λ = 0.1 and freeze the base model weights"

## Why this matters

An isolated chunk loses document context. The section label tells the retriever and the generator where the chunk belongs. This helps the retriever match semantically related chunks and helps the generator answer more accurately.

## The section detector

```python
SECTION_PATTERN = re.compile(
    r'^\s*(?:'
    r'(?:Abstract|Introduction|Background|Related Work|Method(?:ology)?|'
    r'Approach|Experiments?(?:\s+and\s+Results?)?|Results?(?:\s+and\s+Discussion)?|'
    r'Discussion|Conclusion(?:s)?|Limitations|Ethics|Broader Impact|'
    r'Acknowledg(?:e|ment)s?|References|Appendix|Supplementary|'
    r'(?:I{1,3}V?|V?I{0,3})\.\s+\S|'  # Roman numerals
    r'\d+(?:\.\d+)*\s+\S)'  # Numbered sections like "2.1 Setup"
    r')\s*$',
    re.MULTILINE | re.IGNORECASE,
)
```

This regex matches common academic section headers, including Roman numerals and numbered subsections.

## One LLM call, not many

```python
def _summarize_sections(sections, llm=None):
    # Build a single prompt for all sections
    section_descriptions = []
    for i, (name, text) in enumerate(sections):
        preview = text[:300].replace('\n', ' ').strip()
        section_descriptions.append(f"[{i+1}] {name}: {preview}...")

    prompt = f"""... summarize each section in ONE SENTENCE (max 15 words) ..."""
    response = str(llm.complete(prompt)).strip()
```

This is efficient: one batched LLM call per document, regardless of how many sections or chunks there are.

## Honest caveat

This is your **most defensible idea**, but you have **not A/B tested it against plain sentence-window chunking**. You can honestly say:

> "Contextual chunking is a strong design bet: prepending section summaries gives chunks document context without inflating per-chunk cost. But I have not isolated that ablation yet — it is a named next step."

Saying "I have not isolated it yet" sounds like judgment, not a gap. It is a real engineering answer.

## Why this matters in an interview

You can say:

> "Instead of plain fixed-size chunks, I use contextual chunking: detect academic sections, summarize each with one batched LLM call, and prepend the section label to every chunk. This preserves document context so chunks don't lose their meaning when isolated."

## Common trap

**"Doesn't adding the section summary add noise to the embedding?"**

Strong answer: the summary is short (max 15 words) and section names are high-signal. The overhead is one LLM call per document, not per chunk. You haven't isolated the ablation yet, but the design is sound.

## Self-check

1. Why is chunking needed?
2. What are the three chunking strategies?
3. Which is the default and why?
4. What does the section regex detect?
5. How many LLM calls does the summarization step use?
6. What is the honest caveat about contextual chunking?

## Code map

| Concept | File |
|---|---|
| Sentence-window chunking | `backend/app/ingestion/chunker.py` |
| Semantic chunking | `backend/app/ingestion/chunker.py` |
| Contextual chunking | `backend/app/ingestion/chunker.py` |
| Section detection regex | `backend/app/ingestion/chunker.py` (line 10) |
| Section summarization | `backend/app/ingestion/chunker.py` |
| Default chunking strategy | `backend/app/api/routes/documents.py` (line 36) |
