# Lesson 11 — Judgment Layer: Decisions, Traps, and Framing

## What this lesson covers

- How to use your decisions log in interviews
- The most likely traps and strong answers
- The honest "how much AI helped" framing
- Your one-line pitch

## Your decisions log

File: `.ai/decisions.md`

This file is your secret weapon. It contains dated architectural decisions with reasons, alternatives considered, and consequences. In an interview, you can point to it as evidence that your choices were deliberate.

Key decisions to own:

| Decision | Why it matters |
|---|---|
| Monorepo layout | Single source of truth, stronger portfolio story |
| All AI calls through backend | Security — no keys in Android, centralized control |
| FastEmbed default | OpenAI had no billing; local ONNX, zero cost |
| Groq default | OpenAI had no billing; free fast hosted LLM (now `openai/gpt-oss-120b`) |
| Contextual chunking | Preserves document context per chunk |
| Global BM25 at ingest | Earlier per-query rebuild from dense results was semantically wrong |
| Custom LLM-as-judge | RAGAs import was broken; custom gives full control |
| Config parameter | Makes ablation studies trivial |
| Lost-in-the-middle ordering | LLMs attend better to info near the question |

## Likely traps and strong answers

### "Walk me through what happens when I hit /query"

Do not recite the README. Trace the actual flow:

> "The request hits `/api/v1/query`. We read the `config` param, defaulting to `hybrid_rerank`. If `expand=true`, we rewrite the question into variants. Then we retrieve: dense from Qdrant, sparse from BM25, fuse with RRF, optionally rerank with Cohere, assemble context with dedup and lost-in-the-middle ordering, prompt Groq openai/gpt-oss-120b to generate an answer with citations, and finally run a faithfulness check."

### "Why RRF and not just averaging scores?"

> "Dense cosine similarity and BM25 scores live on completely different scales. You can't add a 0.9 cosine score to a 1200 BM25 score. RRF fuses on rank position, so no normalization is needed. We use k=60, the standard constant from the original RRF paper."

### "Is this in production?"

Do not overclaim:

> "Not in production. Qdrant runs in-memory in dev mode (`:memory:`), BM25 is in-process and lost on restart, and there is no auth middleware yet. Docker Compose with persistence is set up, and the path to production would include auth, rate limiting, and moving to paid Groq/OpenAI tiers."

### "What does RAGAs give you here?"

Correct yourself precisely:

> "We don't actually use the RAGAs library. We built a custom LLM-as-judge because RAGAs had a broken `langchain_community.chat_models.vertexai` import. The metrics — faithfulness, relevancy, context precision, context recall — are conceptually similar to RAGAs, but the implementation is our own."

### "What would you fix first?"

Pick one real, named item:

> "I would isolate the contextual chunking ablation — we never tested plain sentence-window chunking against contextual chunking independently, so we don't know how much the section summaries actually help."

Other valid answers:
- The faithfulness judge shares blind spots with the generator.
- BM25 is not persisted across restarts.
- No auth or rate limiting is wired into `main.py`.

### "How much did AI help?"

Be honest and reframe:

> "I used AI agents heavily as an implementation accelerant, working from a spec I maintained in `AGENTS.md` and `.ai/decisions.md`. What I contributed was the design: which five configs to benchmark, defining relevance mechanically instead of subjectively, catching that the BM25 rebuild-per-query approach was wrong, and reading the actual numbers to find the hybrid_rerank regression. The code was AI-assisted; the judgment about what to measure and what it means was mine."

## The one-line pitch (memorize)

> "Most RAG demos are judged by vibes — 'it gave a good answer.' I built ContextIQ to treat retrieval quality as a measurable engineering property: five retrieval configurations benchmarked on a 30-question test set across precision, recall, and MRR, with a faithfulness check on every live query."

## The architecture diagram (draw from memory)

```
Upload PDF/DOCX/TXT
      │
      ▼
Parse pages with pypdf
      │
      ▼
Chunk (contextual)
      │
      ├──────────────┬───────────────┐
      ▼              ▼
Dense embed        BM25 index
      ▼              ▼
Qdrant             global singleton
      │              │
      └──────┬───────┘
             ▼
      RRF fusion
             ▼
   Cohere rerank
             ▼
  Context assembly
             ▼
  Groq openai/gpt-oss-120b generates answer
          ▼
  Faithfulness check
             ▼
  Answer + sources + score
```

## Self-check

1. What is in `.ai/decisions.md` and why is it useful?
2. Why did you build BM25 as a global singleton instead of per query?
3. What is the honest answer to "Is this in production?"
4. How do you answer "What does RAGAs give you?"
5. Say the one-line pitch from memory.
6. Draw the architecture diagram without looking.

## Code map

| Concept | File |
|---|---|
| Decisions log | `.ai/decisions.md` |
| AGENTS instructions | `AGENTS.md` |
| Pitch + numbers | `README.md` |
| Pipeline | `backend/app/api/routes/query.py` |
