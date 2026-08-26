# Lesson 10 — Evaluation: How you know it works

## What this lesson covers

- The two types of evaluation in your project
- Retrieval metrics: P@5, R@5, MRR
- The faithfulness check
- The actual numbers
- The headline insight

## Two evaluation layers

### 1. Retrieval-only metrics (`retrieval_metrics.py`)
- No LLM calls.
- Measures whether the retriever finds the right chunks.
- Ground truth: "the retrieved chunk belongs to the paper the question targets."

### 2. LLM-as-judge faithfulness (`faithfulness.py`)
- Runs on every live query.
- Checks whether the generated answer is supported by the retrieved chunks.
- Returns a score: supported_claims / total_claims.

## Retrieval metrics explained

File: `backend/app/evaluation/retrieval_metrics.py`

### Precision@k (P@k)

Out of the top-k retrieved chunks, how many are relevant?

```python
precision_at_k = relevant_in_top_k / k
```

Example: if 4 out of the top 5 chunks are from the right paper, P@5 = 0.80.

### Recall@k (R@k)

Out of all relevant chunks in the corpus, how many did we find in the top-k?

```python
recall_at_k = relevant_in_top_k / total_relevant
```

Example: if there are 100 relevant chunks total and we found 5 of them, R@5 = 0.05.

### MRR (Mean Reciprocal Rank)

For each question, find the rank of the first relevant chunk. Take the reciprocal (1/rank). Average that across all questions.

```python
for rank, r in enumerate(results, start=1):
    if is_relevant(r, target_paper):
        mrr = 1.0 / rank
        break
```

If the first chunk is always relevant, MRR = 1.0.

## The numbers

From `data/eval/retrieval_metrics.json`:

| Config | P@5 | R@5 | MRR | Avg relevant in top-5 |
|---|---:|---:|---:|---:|
| vector_only | 0.9733 | 0.0996 | 0.9778 | 4.87 |
| vector_rerank | **0.9933** | 0.1016 | **1.0000** | 4.97 |
| hybrid | 0.9533 | 0.1000 | 1.0000 | 4.77 |
| hybrid_rerank | 0.8533 | 0.0931 | 0.9167 | 4.27 |
| long_context | 0.2000 | 0.0099 | 0.3987 | 1.00 |

## How to read these numbers

- **Precision is very high** because each paper's chunks are semantically distinct from other papers. The retriever rarely confuses papers.
- **Recall is low (~10%)** because each paper has many relevant chunks (20 to 173), and you only retrieve the top 5. This is not a failure — it is the definition of recall@5 when the corpus of relevant chunks is large.
- **MRR is high** because the first relevant chunk almost always appears at rank 1.

## The headline insight: hybrid_rerank underperforms

The default config is `hybrid_rerank`, but it is **not** the best.

- `vector_rerank` beats it: P@5 = 0.9933 vs 0.8533.
- `hybrid` beats it: P@5 = 0.9533 vs 0.8533.

Why? The fused list from dense + BM25 is already high quality. Cohere's cross-encoder, trained on general relevance, sometimes disagrees with your evaluation's notion of relevance ("chunk comes from the right paper"). It pushes correct chunks out of the top 5.

This is the single most important finding in your project. Be ready to explain it fluently:

> "The fused list from BM25 + dense is already high quality. Cohere's reranker signal — trained on general semantic relevance — isn't perfectly aligned with our evaluation objective, which is paper-level relevance. In about a third of questions it pushed correct chunks out of the top 5. It taught me that stacking techniques isn't free; each stage must be validated."

## Faithfulness check

File: `backend/app/evaluation/faithfulness.py`

```python
def check_faithfulness(answer, contexts):
    prompt = f"""You are evaluating whether an answer is faithful to the provided contexts.
    CONTEXTS: ...
    ANSWER: ...
    Task: List each distinct claim in the ANSWER. For each claim, state whether it is SUPPORTED or NOT SUPPORTED by the CONTEXTS."""
    response = str(llm.complete(prompt)).strip()
    # Parse TOTAL, SUPPORTED, and UNSUPPORTED claims
```

What it does:
- Sends the answer and the retrieved chunks to the LLM.
- Asks the LLM to extract claims and judge each one.
- Returns a score, supported count, total count, and unsupported claims.

### Limitation

The faithfulness judge is the same model as the generator (Groq openai/gpt-oss-120b). It can share the generator's blind spots. A stronger version would use a different, larger model as judge.

## Why this matters in an interview

You can say:

> "We measure ContextIQ in two ways. Offline, we compute retrieval-only P@5, R@5, and MRR on a 30-question test set — this gives us objective, non-subjective numbers. Online, every answer gets a faithfulness check that scores how many claims are supported by the retrieved sources. The headline finding from our benchmark is that hybrid_rerank, our default config, actually underperforms plain vector_rerank — more pipeline stages are not automatically better."

## Common trap

**"Why is recall so low if precision is so high?"**

Strong answer: recall@5 is mechanically capped. Each paper has 20 to 173 relevant chunks, and we only take the top 5. Precision measures "of the 5 we picked, how many are right?" — and the answer is almost all of them. Recall measures "of all possible relevant chunks, how many did we find?" — and the answer is a small fraction by definition.

## Self-check

1. What are the three retrieval metrics and what do they measure?
2. Why is precision high but recall low?
3. Which config performed best? Worst?
4. What is the headline insight from your numbers?
5. How does the faithfulness check work?
6. What is a limitation of the faithfulness check?

## Code map

| Concept | File |
|---|---|
| Retrieval metrics | `backend/app/evaluation/retrieval_metrics.py` |
| Metric results | `data/eval/retrieval_metrics.json` |
| Faithfulness check | `backend/app/evaluation/faithfulness.py` |
| Test set | `data/eval/test_set.json` |
