# ContextIQ — LinkedIn Portfolio Post

## Ready-to-post copy

I assumed that a more elaborate RAG pipeline would produce better retrieval.

ContextIQ proved me wrong.

I benchmarked five retrieval configurations for my research-paper QA system on 30 questions across five papers. The pipeline with dense retrieval plus a Cohere reranker achieved **P@5 = 0.9933** and **MRR = 1.0000**.

When I added BM25, Reciprocal Rank Fusion, *and then* the same reranker, performance dropped to **P@5 = 0.8533** and **MRR = 0.9167**—while latency stayed roughly the same (~179 s in this benchmark).

The result changed my default engineering instinct:

> More retrieval stages do not automatically mean better retrieval.

Hybrid retrieval is still useful for exact terms and acronyms. But in this evaluation, fusion added candidates whose cross-encoder ordering did not align with the benchmark's paper-level relevance signal. The simpler `hybrid` configuration was the best speed/quality trade-off: MRR 1.0 at ~3.6 s.

So I kept five selectable configurations behind one FastAPI endpoint, added P@5, R@5, and MRR evaluation, and made pipeline choices measurable instead of aesthetic.

ContextIQ is a RAG backend for research papers: PDF upload, Qdrant + BM25 retrieval, cited answers, a claim-level faithfulness check, Docker Compose, and 39 pytest tests.

The lesson I’m taking into every AI project: **measure the pipeline you have, not the pipeline that sounds most sophisticated.**

#RAG #GenerativeAI #MachineLearning #FastAPI #InformationRetrieval #MLOps

## Evidence and wording notes

- Metrics are retrieval-only results from `data/eval/retrieval_metrics.json`, measured across 30 questions × 5 paper documents. They are **not** LLM-judge scores.
- Relevance is defined as a retrieved chunk belonging to the question's target paper. This is intentionally disclosed because it is a paper-level relevance proxy, not human passage-level annotation.
- The comparison is `vector_rerank` (P@5 0.9933, MRR 1.0000) versus `hybrid_rerank` (P@5 0.8533, MRR 0.9167). `hybrid` reached MRR 1.0000 with P@5 0.9533.
- Keep the latency values qualified as “in this benchmark”; Cohere trial throttling materially affects them.

## Live-query screenshot checklist

Use this as the image paired with the post. Capture the terminal or API client only after the response is returned; crop out the upload step, environment variables, and any request headers.

```bash
cd /home/mad/StudioProjects/ContextIQ/backend
source venv/bin/activate
uvicorn app.main:app --reload --port 8001

# In a second terminal, after uploading the DPO PDF:
curl -sS -X POST http://127.0.0.1:8001/api/v1/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"What does DPO stand for and what problem does it solve?","top_k":3,"config":"vector_only"}' \
  | python -m json.tool
```

The crop should show only: the question, concise answer, source filenames/pages, and metadata with `model: openai/gpt-oss-120b`. Do not claim the screenshot is a benchmark result; it is a product demonstration.
