"""Subset evaluation with combined LLM-as-judge to fit free-tier limits.

Uses 5 representative questions (one per paper) across all 5 configs.
Each question-config needs only 2 LLM calls (generation + combined judge).
"""

import os, json, time, glob
import re
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
from app.evaluation.ragas_runner import load_test_set
from app.core.llm import get_llm


def combined_judge(llm, question, answer, contexts, ground_truth) -> dict:
    """One LLM call returns all four scores."""
    context_block = "\n\n---\n\n".join(f"[Context {i+1}]\n{c[:600]}" for i, c in enumerate(contexts[:5]))

    prompt = f"""You are evaluating a RAG system. Rate each dimension from 0.0 to 1.0.

QUESTION:
{question}

GROUND TRUTH ANSWER:
{ground_truth}

GENERATED ANSWER:
{answer}

RETRIEVED CONTEXTS:
{context_block}

Rate:
- Faithfulness: is the generated answer fully supported by the contexts?
- AnswerRelevancy: does the generated answer address the question?
- ContextPrecision: are the retrieved contexts relevant to the question?
- ContextRecall: do the retrieved contexts contain the ground truth information?

Format EXACTLY as:
Faithfulness: X.XX
AnswerRelevancy: X.XX
ContextPrecision: X.XX
ContextRecall: X.XX"""

    try:
        response = str(llm.complete(prompt)).strip()
        scores = {}
        for key in ["Faithfulness", "AnswerRelevancy", "ContextPrecision", "ContextRecall"]:
            match = re.search(rf'{key}:\s*(\d+\.?\d*)', response)
            val = float(match.group(1)) if match else 0.0
            scores[key.lower()] = max(0.0, min(1.0, val))
        return scores
    except Exception as e:
        print(f"Judge failed: {e}")
        return {"faithfulness": 0.0, "answer_relevancy": 0.0, "context_precision": 0.0, "context_recall": 0.0}


def main():
    settings = get_settings()
    qdrant = get_qdrant_client()
    ensure_collection(qdrant)
    embed_model = get_embed_model()
    llm = get_llm()

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

    # Load test set and pick 5 representative questions (one per paper)
    full_test_set = load_test_set()
    selected = [full_test_set[i] for i in [0, 6, 12, 18, 24]]  # q01, q07, q13, q19, q25
    print(f'Selected {len(selected)} questions (one per paper)\n')

    all_results = {}

    for config_name in CONFIGS:
        print(f'\n{"="*60}')
        print(f'Running config: {config_name}')
        print(f'{"="*60}')

        query_fn = CONFIGS[config_name]
        config_scores = []

        for item in selected:
            print(f"  {item['id']}: {item['question'][:70]}...")
            start = time.time()
            try:
                answer, contexts = query_fn(item["question"])
                scores = combined_judge(llm, item["question"], answer, contexts, item["ground_truth"])
                scores["question_id"] = item["id"]
                scores["question"] = item["question"]
                scores["answer"] = answer[:300]
                scores["latency_ms"] = round((time.time() - start) * 1000, 2)
                config_scores.append(scores)
                print(f"    F={scores['faithfulness']:.2f} R={scores['answer_relevancy']:.2f} P={scores['context_precision']:.2f} C={scores['context_recall']:.2f}")
            except Exception as e:
                print(f"    ERROR: {e}")
                config_scores.append({
                    "question_id": item["id"],
                    "error": str(e),
                    "faithfulness": 0.0, "answer_relevancy": 0.0,
                    "context_precision": 0.0, "context_recall": 0.0,
                })

        all_results[config_name] = config_scores

    # Compute averages
    summary = []
    for config_name, scores in all_results.items():
        valid = [s for s in scores if not s.get("error")]
        avg = {
            "config": config_name,
            "faithfulness": round(sum(s["faithfulness"] for s in valid) / len(valid), 4) if valid else 0,
            "answer_relevancy": round(sum(s["answer_relevancy"] for s in valid) / len(valid), 4) if valid else 0,
            "context_precision": round(sum(s["context_precision"] for s in valid) / len(valid), 4) if valid else 0,
            "context_recall": round(sum(s["context_recall"] for s in valid) / len(valid), 4) if valid else 0,
            "avg_latency_ms": round(sum(s.get("latency_ms", 0) for s in valid) / len(valid), 2) if valid else 0,
        }
        summary.append(avg)

    print(f'\n\n{"="*80}')
    print('SUBSET EVALUATION TABLE (5 questions × 5 configs)')
    print(f'{"="*80}')
    print(f'{"Config":<20} {"Faith":>8} {"Relav":>8} {"Prec":>8} {"Recall":>8} {"AvgMs":>8}')
    print(f'{"-"*20} {"-"*8} {"-"*8} {"-"*8} {"-"*8} {"-"*8}')
    for s in summary:
        print(f'{s["config"]:<20} {s["faithfulness"]:>8.4f} {s["answer_relevancy"]:>8.4f} {s["context_precision"]:>8.4f} {s["context_recall"]:>8.4f} {s["avg_latency_ms"]:>8.0f}')

    # Save
    results_dir = '/home/mad/StudioProjects/ContextIQ/data/eval'
    os.makedirs(results_dir, exist_ok=True)
    output = {
        "note": "Subset evaluation: 5 representative questions (one per paper) × 5 configs. Combined single-call judge.",
        "summary": summary,
        "per_question": all_results,
    }
    output_path = os.path.join(results_dir, 'subset_results.json')
    with open(output_path, 'w') as f:
        json.dump(output, f, indent=2)
    print(f'\nSaved to {output_path}')


if __name__ == '__main__':
    main()
