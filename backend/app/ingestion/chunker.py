from llama_index.core.node_parser import (
    SentenceSplitter,
    SemanticSplitterNodeParser,
)
from llama_index.core import Document as LlamaDocument
from llama_index.embeddings.openai import OpenAIEmbedding


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
