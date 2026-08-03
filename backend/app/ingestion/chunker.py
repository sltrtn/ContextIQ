import re
from llama_index.core.node_parser import (
    SentenceSplitter,
    SemanticSplitterNodeParser,
)
from llama_index.core import Document as LlamaDocument
from llama_index.embeddings.openai import OpenAIEmbedding


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


def sentence_window_chunker(
    text: str,
    chunk_size: int = 512,
    chunk_overlap: int = 50,
) -> list[dict]:
    """Split text into chunks using sentence-window strategy."""
    splitter = SentenceSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
    )
    doc = LlamaDocument(text=text)
    nodes = splitter.get_nodes_from_documents([doc])
    return [
        {
            "text": node.text,
            "node_id": node.node_id,
        }
        for node in nodes
    ]


def semantic_chunker(
    text: str,
    buffer_size: int = 1,
    breakpoint_percentile_threshold: int = 95,
) -> list[dict]:
    """Split text using semantic similarity between sentences."""
    embed_model = OpenAIEmbedding(model="text-embedding-3-small")
    splitter = SemanticSplitterNodeParser(
        buffer_size=buffer_size,
        breakpoint_percentile_threshold=breakpoint_percentile_threshold,
        embed_model=embed_model,
    )
    doc = LlamaDocument(text=text)
    nodes = splitter.get_nodes_from_documents([doc])
    return [
        {
            "text": node.text,
            "node_id": node.node_id,
        }
        for node in nodes
    ]


def _detect_sections(text: str) -> list[tuple[str, str]]:
    """Detect section boundaries in text. Returns list of (section_name, section_text)."""
    lines = text.split('\n')
    sections = []
    current_name = "Preamble"
    current_lines = []

    for line in lines:
        stripped = line.strip()
        if stripped and SECTION_PATTERN.match(stripped):
            if current_lines:
                sections.append((current_name, '\n'.join(current_lines)))
            current_name = stripped
            current_lines = []
        else:
            current_lines.append(line)

    if current_lines:
        sections.append((current_name, '\n'.join(current_lines)))

    return sections


def _summarize_sections(sections: list[tuple[str, str]], llm=None) -> dict[str, str]:
    """Generate a short summary for each section using the LLM.

    Returns dict mapping section_name -> summary.
    Batches sections into one LLM call to minimize API calls.
    """
    if llm is None:
        from app.core.llm import get_llm
        llm = get_llm()

    if len(sections) <= 1:
        return {sections[0][0]: ""} if sections else {}

    # Build a single prompt for all sections
    section_descriptions = []
    for i, (name, text) in enumerate(sections):
        # Use first 300 chars of each section as preview
        preview = text[:300].replace('\n', ' ').strip()
        section_descriptions.append(f"[{i+1}] {name}: {preview}...")

    prompt = f"""You are summarizing sections of a research paper for use as context labels.

Here are the sections:
{chr(10).join(section_descriptions)}

For EACH section, write a ONE-SENTENCE summary (max 15 words) describing what that section covers.
Format your response EXACTLY as:
[1] summary text
[2] summary text
...etc

Do NOT include the section names, just the summaries."""

    try:
        response = str(llm.complete(prompt)).strip()
        summaries = {}
        for line in response.split('\n'):
            line = line.strip()
            match = re.match(r'\[(\d+)\]\s*(.*)', line)
            if match:
                idx = int(match.group(1)) - 1
                if 0 <= idx < len(sections):
                    summaries[sections[idx][0]] = match.group(2).strip()
        # Fill in missing sections
        for name, _ in sections:
            if name not in summaries:
                summaries[name] = ""
        return summaries
    except Exception as e:
        print(f"Section summarization failed: {e}")
        return {name: "" for name, _ in sections}


def contextual_chunker(
    pages: list[dict],
    chunk_size: int = 512,
    chunk_overlap: int = 50,
    llm=None,
) -> list[dict]:
    """Contextual chunking: detect sections, summarize each, prepend summary to chunks.

    Each chunk becomes: "[Section: section_name — summary] chunk_text"
    """
    if llm is None:
        from app.core.llm import get_llm
        llm = get_llm()

    # Combine all page text to detect sections across the whole document
    full_text = '\n\n'.join(p["text"] for p in pages)
    sections = _detect_sections(full_text)

    if not sections:
        # Fallback: no sections detected, use standard chunking
        return chunk_pages(pages, strategy="sentence_window", chunk_size=chunk_size, chunk_overlap=chunk_overlap)

    # Summarize sections (one LLM call)
    print(f"  Summarizing {len(sections)} sections...")
    summaries = _summarize_sections(sections, llm)

    # Now chunk each page, but track which section each chunk belongs to
    all_chunks = []
    for page in pages:
        page_text = page["text"]
        page_sections = _detect_sections(page_text)

        # If no sections on this page, use the most recent section from full text
        if not page_sections:
            # Find which section this page belongs to by checking overlap
            for sec_name, sec_text in reversed(sections):
                if sec_text[:100] in page_text or page_text[:100] in sec_text:
                    page_sections = [(sec_name, page_text)]
                    break
            if not page_sections:
                page_sections = [("Unknown", page_text)]

        for sec_name, sec_text in page_sections:
            chunks = sentence_window_chunker(sec_text, chunk_size=chunk_size, chunk_overlap=chunk_overlap)
            summary = summaries.get(sec_name, "")
            section_label = f"[Section: {sec_name}" + (f" — {summary}]" if summary else "]")

            for c in chunks:
                c["text"] = f"{section_label}\n\n{c['text']}"
                c["page_number"] = page["page_number"]
                c["section"] = sec_name
                all_chunks.append(c)

    return all_chunks


def chunk_pages(
    pages: list[dict],
    strategy: str = "sentence_window",
    chunk_size: int = 512,
    chunk_overlap: int = 50,
    llm=None,
) -> list[dict]:
    """Chunk a list of page dicts ({text, page_number}) into chunks with page metadata."""
    if strategy == "contextual":
        return contextual_chunker(pages, chunk_size=chunk_size, chunk_overlap=chunk_overlap, llm=llm)

    all_chunks = []
    for page in pages:
        chunks = chunk_document(page["text"], strategy=strategy, chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        for c in chunks:
            c["page_number"] = page["page_number"]
        all_chunks.extend(chunks)
    return all_chunks


def chunk_document(
    text: str,
    strategy: str = "sentence_window",
    chunk_size: int = 512,
    chunk_overlap: int = 50,
) -> list[dict]:
    """Chunk document text using the specified strategy."""
    if strategy == "semantic":
        return semantic_chunker(text)
    return sentence_window_chunker(
        text, chunk_size=chunk_size, chunk_overlap=chunk_overlap
    )
