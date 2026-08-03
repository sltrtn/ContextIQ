"""Full evaluation run — all configs against the test set."""

import os, json, time, glob
from dotenv import load_dotenv

load_dotenv()

os.environ['QDRANT_URL'] = ':memory:'
if not os.environ.get('GROQ_API_KEY'):
    raise ValueError("Set GROQ_API_KEY in backend/.env or environment before running this script")

from app.core.config import get_settings
from app.ingestion.parser import parse_document_pages
from app.ingestion.chunker import chunk_pages
from app.retrieval.dense import get_qdrant_client, ensure_collection
from app.core.embeddings import get_embed_model
from app.retrieval.sparse import build_global_bm25
from llama_index.core import Document as LlamaDocument, VectorStoreIndex, StorageContext
from llama_index.vector_stores.qdrant import QdrantVectorStore
from app.evaluation.configs import CONFIGS
from app.evaluation.ragas_runner import run_evaluation, load_test_set


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
        chunks = chunk_pages(pages, strategy='contextual')
        all_chunks.extend([(fname, c) for c in chunks])
        print(f'  {len(chunks)} chunks')

    print(f'\nTotal chunks: {len(all_chunks)}')

    docs = [LlamaDocument(text=c[1]['text'], metadata={'filename': c[0], 'page_number': c[1].get('page_number', 0)}) for c in all_chunks]
    vector_store = QdrantVectorStore(client=qdrant, collection_name=settings.qdrant_collection)
    storage_context = StorageContext.from_defaults(vector_store=vector_store)
    VectorStoreIndex.from_documents(docs, storage_context=storage_context, embed_model=embed_model)
    build_global_bm25([c[1] for c in all_chunks])
    print(f'Ingested {len(all_chunks)} chunks into Qdrant\n')

    # Load test set
    test_set = load_test_set()
    print(f'Test set: {len(test_set)} questions\n')

    # Run each config
    all_summaries = []
    for config_name in CONFIGS:
        print(f'\n{"="*60}')
        print(f'Running config: {config_name}')
        print(f'{"="*60}')

        query_fn = CONFIGS[config_name]
        start = time.time()
        summary = run_evaluation(config_name=config_name, query_fn=query_fn, test_set=test_set)
        elapsed = time.time() - start

        print(f'\n  Faithfulness:   {summary.faithfulness:.4f}')
        print(f'  Relevancy:      {summary.answer_relevancy:.4f}')
        print(f'  Precision:      {summary.context_precision:.4f}')
        print(f'  Recall:         {summary.context_recall:.4f}')
        print(f'  Latency p50:    {summary.latency_p50_ms:.0f}ms')
        print(f'  Latency p95:    {summary.latency_p95_ms:.0f}ms')
        print(f'  Time elapsed:   {elapsed:.1f}s')

        errors = [r for r in summary.results if r.error]
        if errors:
            print(f'  Errors: {len(errors)}')
            for e in errors[:3]:
                print(f'    {e.question_id}: {e.error[:80]}')

        all_summaries.append(summary)

    # Print comparison table
    print(f'\n\n{"="*80}')
    print('COMPARISON TABLE')
    print(f'{"="*80}')
    print(f'{"Config":<20} {"Faith":>8} {"Relav":>8} {"Prec":>8} {"Recall":>8} {"P50ms":>8} {"P95ms":>8}')
    print(f'{"-"*20} {"-"*8} {"-"*8} {"-"*8} {"-"*8} {"-"*8} {"-"*8}')
    for s in all_summaries:
        print(f'{s.config:<20} {s.faithfulness:>8.4f} {s.answer_relevancy:>8.4f} {s.context_precision:>8.4f} {s.context_recall:>8.4f} {s.latency_p50_ms:>8.0f} {s.latency_p95_ms:>8.0f}')

    # Save results
    results_dir = '/home/mad/StudioProjects/ContextIQ/data/eval'
    os.makedirs(results_dir, exist_ok=True)

    results_data = []
    for s in all_summaries:
        results_data.append({
            'config': s.config,
            'faithfulness': round(s.faithfulness, 4),
            'answer_relevancy': round(s.answer_relevancy, 4),
            'context_precision': round(s.context_precision, 4),
            'context_recall': round(s.context_recall, 4),
            'latency_p50_ms': round(s.latency_p50_ms, 2),
            'latency_p95_ms': round(s.latency_p95_ms, 2),
            'num_questions': s.num_questions,
            'questions': [
                {
                    'id': r.question_id,
                    'question': r.question,
                    'answer': r.answer[:500],
                    'faithfulness': round(r.faithfulness, 4),
                    'answer_relevancy': round(r.answer_relevancy, 4),
                    'context_precision': round(r.context_precision, 4),
                    'context_recall': round(r.context_recall, 4),
                    'latency_ms': round(r.latency_ms, 2),
                    'error': r.error,
                }
                for r in s.results
            ],
        })

    output_path = os.path.join(results_dir, 'results.json')
    with open(output_path, 'w') as f:
        json.dump(results_data, f, indent=2)
    print(f'\nResults saved to {output_path}')


if __name__ == '__main__':
    main()
