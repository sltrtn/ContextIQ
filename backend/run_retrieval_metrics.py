"""Run retrieval-only metrics across all 30 questions × 5 configs.

Fast, zero API cost. Produces P@5, R@5, MRR comparison table.
"""

import os, json, glob, time

os.environ['QDRANT_URL'] = ':memory:'
os.environ['OPENAI_API_KEY'] = 'dummy'
os.environ['GROQ_API_KEY'] = 'dummy'

from app.core.config import get_settings
from app.ingestion.parser import parse_document_pages
from app.ingestion.chunker import chunk_pages
from app.retrieval.dense import get_qdrant_client, ensure_collection
from app.core.embeddings import get_embed_model
from app.retrieval.sparse import build_global_bm25
from llama_index.core import Document as LlamaDocument, VectorStoreIndex, StorageContext
from llama_index.vector_stores.qdrant import QdrantVectorStore
from app.evaluation.retrieval_metrics import RETRIEVAL_CONFIGS, compute_metrics
from app.evaluation.ragas_runner import load_test_set


def main():
    settings = get_settings()
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)
    embed_model = get_embed_model()

    # Ingest all papers
    paper_files = sorted(glob.glob('/home/mad/StudioProjects/ContextIQ/data/papers/*.pdf'))
    all_chunks = []
    for fp in paper_files:
        fname = os.path.basename(fp)
        print(f'Parsing {fname}...')
        pages = parse_document_pages(fp)
        chunks = chunk_pages(pages, strategy='sentence_window')
        all_chunks.extend([(fname, c) for c in chunks])
        print(f'  {len(chunks)} chunks')

    print(f'\nTotal chunks: {len(all_chunks)}')

    docs = [LlamaDocument(text=c[1]['text'], metadata={'filename': c[0], 'page_number': c[1].get('page_number', 0)}) for c in all_chunks]
    vector_store = QdrantVectorStore(client=qdrant, collection_name=settings.qdrant_collection)
    storage_context = StorageContext.from_defaults(vector_store=vector_store)
    VectorStoreIndex.from_documents(docs, storage_context=storage_context, embed_model=embed_model)
    build_global_bm25([c[1] for c in all_chunks])
    print(f'Ingested {len(all_chunks)} chunks into Qdrant\n')

    test_set = load_test_set()
    print(f'Test set: {len(test_set)} questions\n')

    # Precompute total chunks per paper for accurate recall
    paper_chunk_counts = {}
    for fname, chunk in all_chunks:
        paper_chunk_counts[fname] = paper_chunk_counts.get(fname, 0) + 1
    print('Chunks per paper:', paper_chunk_counts)

    summary = []
    per_question = {}

    for config_name, retrieve_fn in RETRIEVAL_CONFIGS.items():
        print(f'Running {config_name}...')
        start = time.time()
        scores = []
        per_question[config_name] = []

        for item in test_set:
            results = retrieve_fn(item['question'])
            total_relevant = paper_chunk_counts.get(item['paper'], 0)
            metrics = compute_metrics(results, item['paper'], k=5, total_relevant_in_corpus=total_relevant)
            metrics['question_id'] = item['id']
            metrics['question'] = item['question']
            scores.append(metrics)
            per_question[config_name].append(metrics)

        elapsed = time.time() - start
        avg = {
            'config': config_name,
            'precision_at_5': round(sum(s['precision_at_k'] for s in scores) / len(scores), 4),
            'recall_at_5': round(sum(s['recall_at_k'] for s in scores) / len(scores), 4),
            'mrr': round(sum(s['mrr'] for s in scores) / len(scores), 4),
            'avg_relevant_in_top_5': round(sum(s['relevant_in_top_k'] for s in scores) / len(scores), 2),
            'avg_total_relevant': round(sum(s['total_relevant'] for s in scores) / len(scores), 2),
            'time_ms': round(elapsed * 1000, 2),
        }
        summary.append(avg)
        print(f"  P@5={avg['precision_at_5']:.4f} R@5={avg['recall_at_5']:.4f} MRR={avg['mrr']:.4f} ({elapsed:.1f}s)")

    print(f'\n\n{"="*80}')
    print('RETRIEVAL METRICS TABLE (30 questions × 5 configs)')
    print(f'{"="*80}')
    print(f'{"Config":<20} {"P@5":>10} {"R@5":>10} {"MRR":>10} {"AvgRelevant":>14} {"Time(ms)":>10}')
    print(f'{"-"*20} {"-"*10} {"-"*10} {"-"*10} {"-"*14} {"-"*10}')
    for s in summary:
        print(f'{s["config"]:<20} {s["precision_at_5"]:>10.4f} {s["recall_at_5"]:>10.4f} {s["mrr"]:>10.4f} {s["avg_relevant_in_top_5"]:>14.2f} {s["time_ms"]:>10.0f}')

    # Save
    results_dir = '/home/mad/StudioProjects/ContextIQ/data/eval'
    os.makedirs(results_dir, exist_ok=True)
    output_path = os.path.join(results_dir, 'retrieval_metrics.json')
    with open(output_path, 'w') as f:
        json.dump({
            'note': 'Retrieval-only metrics: Precision@5, Recall@5, MRR across 30 questions and 5 configs. No LLM calls.',
            'summary': summary,
            'per_question': per_question,
        }, f, indent=2)
    print(f'\nSaved to {output_path}')


if __name__ == '__main__':
    main()
