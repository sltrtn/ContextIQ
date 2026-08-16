# Lesson 9 — The Query Endpoint: Five Configs in One Route

## What this lesson covers

- The `/query` endpoint
- The five pipeline configs
- Query expansion
- Streaming response
- Faithfulness hookup

## The endpoint

File: `backend/app/api/routes/query.py`

```python
@router.post("/query", response_model=QueryResponse)
async def query(req: QueryRequest):
    start = time.time()
    config = req.config or "hybrid_rerank"

    queries = [req.question]
    if req.expand:
        queries = expand_query(req.question)

    llm = get_llm()

    if config == "vector_only":
        ...
    elif config == "vector_rerank":
        ...
    elif config == "hybrid":
        ...
    elif config == "long_context":
        ...
    else:  # hybrid_rerank (default)
        ...

    latency = time.time() - start
    faith = check_faithfulness(str(answer), context_texts)

    return QueryResponse(
        answer=str(answer),
        sources=sources,
        metadata={...},
        faithfulness=faith,
    )
```

## The five configs

All five are behind **one endpoint** with a `config` parameter.

### `vector_only`
- Dense retrieval only.
- No rerank, no BM25.
- Result: P@5 = 0.9733, R@5 = 0.0996, MRR = 0.9778.

### `vector_rerank`
- Dense retrieval + Cohere rerank.
- No BM25.
- Result: P@5 = 0.9933, R@5 = 0.1016, MRR = 1.0000.
- **Best precision and MRR in your benchmark.**

### `hybrid`
- Dense + BM25 + RRF.
- No reranking.
- Result: P@5 = 0.9533, R@5 = 0.1000, MRR = 1.0000.

### `hybrid_rerank` (default)
- Dense + BM25 + RRF + Cohere rerank.
- Result: P@5 = 0.8533, R@5 = 0.0931, MRR = 0.9167.
- **Worse than hybrid and vector_rerank** — the headline insight.

### `long_context`
- No retrieval at all.
- Stuffs as many chunks as fit into the prompt (~100k chars).
- Control group to prove retrieval matters.
- Result: P@5 = 0.2000, R@5 = 0.0099, MRR = 0.3987.

## Why one endpoint?

From your decisions log:

> "A single endpoint with a config parameter makes ablation studies trivial — same API, same request format, just different retrieval strategies."

This lets the frontend switch pipelines by changing one field and lets the evaluation runner compare configs consistently.

## Query expansion

File: `backend/app/retrieval/query_transform.py`

```python
def expand_query(query, num_variants=2):
    prompt = f"""Rewrite the following question in {num_variants} different ways to improve search retrieval..."""
    response = str(llm.complete(prompt)).strip()
    variants = []
    for line in response.split('\n'):
        match = re.match(r'\[(\d+)\]\s*(.*)', line):
            if match:
                text = match.group(2).strip()
                if text and text != query:
                    variants.append(text)
    return [query] + variants[:num_variants]
```

What it does:
- Asks the LLM to rewrite the question in 2 alternative ways.
- Retrieves for the original + variants.
- Re-fuses the results with RRF.
- Falls back to the original query if the LLM call fails.

This improves recall by covering different vocabulary.

## Streaming endpoint

File: `backend/app/api/routes/query.py`

```python
@router.post("/query/stream")
async def query_stream(req: QueryRequest):
    async def event_stream():
        dense_results = _dense_retrieve(index, req.question, top_k=20)
        yield f"data: {{\"event\":\"dense\",\"count\":{len(dense_results)}}}\n\n"
        ...
```

The streaming endpoint returns Server-Sent Events (SSE). It emits progress events:
- `dense` — number of dense results
- `sparse` — number of BM25 results
- `fusion` — number of fused results
- `rerank` — number of reranked results
- `token` — streamed answer tokens
- `done` — final sources

## Faithfulness hookup

```python
context_texts = [s.text for s in sources]
faith = check_faithfulness(str(answer), context_texts)
```

After the answer is generated, the system runs a second LLM call to check whether the answer's claims are supported by the retrieved sources. This is returned in the response.

## Why this matters in an interview

You can say:

> "The `/query` endpoint supports five pipeline configs via a single `config` parameter. I can switch between dense-only, dense-with-rerank, hybrid, hybrid-with-rerank, and long-context. This makes ablation studies trivial. The default is hybrid_rerank, though our own benchmark showed that vector_rerank actually performed better — which is the key finding. Every answer also gets a faithfulness check."

## Common trap

**"Which config is best?"**

Strong answer: for our 30-question benchmark, **vector_rerank** had the best P@5 (0.9933) and MRR (1.0). Surprisingly, adding BM25 *and* rerank (hybrid_rerank) hurt performance. This shows that stacking techniques is not free — every stage must be validated.

## Self-check

1. What is the default config?
2. List the five configs and what each includes.
3. Which config performed best on your benchmark?
4. Which config performed worst?
5. What does query expansion do?
6. What does the streaming endpoint emit before the answer tokens?
7. When does the faithfulness check run?

## Code map

| Concept | File |
|---|---|
| Query endpoint | `backend/app/api/routes/query.py` |
| Config switching | `backend/app/api/routes/query.py` (line 118) |
| Query expansion | `backend/app/retrieval/query_transform.py` |
| Streaming endpoint | `backend/app/api/routes/query.py` (line 217) |
| Faithfulness call | `backend/app/api/routes/query.py` (line 201) |
