# Lesson 14 — Corrections to Claude's Interview Guide

The guide Claude gave you is mostly accurate, but it contains at least one concrete error and one imprecise statement that could hurt you in an interview if memorized verbatim. This file lists what to correct.

## Verified correction

### 1. Test count: 39, not 33

Claude's guide says:

> "33 unit tests, mocked LLM calls where relevant."

**Correct statement:**

> ContextIQ has **39 pytest tests** in `backend/tests/`, and they all pass.

How I verified it:

```bash
cd backend && source venv/bin/activate && python -m pytest tests -q --no-header
# Output: 39 passed in 9.72s
```

If an interviewer asks "how many tests do you have?" say **39**.

## Verified correct

The following facts in Claude's guide match your actual code and data:

- RRF constant k=60 — correct (`backend/app/retrieval/fusion.py` line 3)
- Cohere rate limit: 10 calls/minute, handled with 6.1s gap — correct
- `hybrid_rerank` is the default config — correct (`backend/app/api/routes/query.py` line 118)
- 30-question test set across 5 papers — correct (verified filenames in `data/papers/` and questions in `data/eval/test_set.json`)
- The metrics table values match `data/eval/retrieval_metrics.json`
- The 5 pipeline configs and descriptions are correct
- The architecture diagram is correct

## Slightly imprecise statement

Claude's guide says:

> "vector_rerank | **0.993** | ..."

The actual stored value is **0.9933**. Saying "0.993" is fine for an interview, but if you want to be precise, say "0.9933".

## What to do with Claude's guide

Use it as a narrative scaffold, but cross-check facts against:
- These study files
- Your actual code files
- `data/eval/retrieval_metrics.json`

The rule: if you cannot connect a claim to a file or a number in the repo, do not memorize it.
