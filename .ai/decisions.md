# ContextIQ — Architectural Decisions

> Every important decision with date, reason, alternatives, and consequences.

---

## 2026-07-02 — Monorepo Layout

**Decision:** Keep all projects (Android, Backend, Frontend) in a single git repository.

**Reason:** Single source of truth for the entire system. Shared documentation, issue tracking, and CI. Portfolio story is stronger when all three components are visibly part of one project.

**Alternatives Considered:**
- Separate repos per component (Android / Backend / Frontend) — rejected because it fragments the story and adds overhead.

**Consequences:**
- Git repo will be larger.
- Android project currently at root; will be moved to `android/` subdirectory.
- Need to ensure Android Studio opens correctly with the subdirectory.

---

## 2026-07-02 — All AI Calls Through Backend

**Decision:** No direct AI API calls from the Android app. All AI processing goes through the ContextIQ backend.

**Reason:** Security — API keys on mobile devices can be extracted. Centralized control — can swap models, add caching, enforce rate limits. Observability — backend can log and measure everything.

**Alternatives Considered:**
- Direct Gemini/Sarvam calls from Android (Scholium's approach) — rejected because hardcoded key was compromised.
- On-device ML — rejected for quality and scope reasons.

**Consequences:**
- Android app is fully dependent on backend availability.
- All 14 screens rewired from direct HTTP to Retrofit `ContextIQClient.api`.
- Backend must implement all 13 endpoints before Android works.

---

## 2026-07-02 — Scholarly Navy (#002855) as Hero Color

**Decision:** Use Scholarly Navy (#002855) as the primary brand color, replacing Meluko's amber.

**Reason:** Evokes libraries, academic rigour, ink on paper. Differentiates ContextIQ from Meluko while keeping the same design system structure.

**Alternatives Considered:**
- Meluko's amber (#D4A017) — rejected because it feels more creative/design than academic.
- Deep green — considered but doesn't fit the research paper theme.

**Consequences:**
- All Android theme colors updated.
- Dark and light schemes use Scholarly Navy as primary.
- Web frontend will use same color token (#002855).

---

## 2026-07-02 — Railway for Deployment

**Decision:** Deploy backend on Railway using Docker Compose.

**Reason:** Railway has good free tier, simple Docker Compose support, handles Redis add-on, and is demo-friendly with public URLs.

**Alternatives Considered:**
- Render — good but slightly more configuration for Docker Compose.
- Fly.io — more complex, overkill for this stage.
- AWS/GCP — too much overhead for a portfolio project.

**Consequences:**
- Backend must be fully containerized (Dockerfile + docker-compose.yml).
- Railway will need the `.env` variables configured in their dashboard.

---

## 2026-07-02 — Three Retrieval Configurations for RAGAs

**Decision:** Benchmark exactly three configurations — Naive RAG, Dense Only, and Hybrid + Rerank.

**Reason:** Three is enough to show a clear improvement curve. Adding more (e.g., varying chunk sizes, embedding models) would dilute the story.

**Alternatives Considered:**
- Single configuration — no comparison, no story.
- Five+ configurations — too many to present cleanly.

**Consequences:**
- RAGAs pipeline must support switching between configs.
- Test set (30 questions) must be runnable against all three.
- The benchmark table is the centerpiece of the README and resume.

---

## 2026-07-02 — Meluko Design Language (adapted)

**Decision:** Use Meluko's design patterns (Clash Display, 0dp elevation, 20-24dp rounding, spring animations, uppercase headers with letter spacing, no drag handle on bottom sheets) adapted with Scholarly Navy color scheme.

**Reason:** Meluko's design language is polished and distinctive. Adapting it (rather than inventing from scratch) saves time while creating visual consistency across both projects.

**Alternatives Considered:**
- Material You default styling — rejected, looks generic.
- Custom design from scratch — too time-consuming for a portfolio project.

**Consequences:**
- Clash Display fonts bundled in APK (6 weights, ~2MB).
- All Compose components use `RoundedCornerShape` with 20-24dp.
- Buttons are 56dp tall with 20dp rounding.
- `pressScale()` modifier adds spring animation to all interactive elements.
- Bottom sheets have `dragHandle = null`.

---

## 2026-07-04 — FastEmbed as Local Embedding Fallback

**Decision:** Add `EMBEDDING_PROVIDER` env var supporting `fastembed` (local ONNX, 384 dims) and `openai` (1536 dims). Default to `fastembed` during development.

**Reason:** OpenAI key has no billing credits. fastembed uses BAAI/bge-small-en-v1.5 via ONNX runtime — fast, zero API cost, works fully offline. No quality loss for development testing.

**Alternatives Considered:**
- sentence-transformers — heavier, requires PyTorch
- Wait for OpenAI billing — blocks all development

**Consequences:**
- Qdrant collection size is 384 when fastembed, 1536 when OpenAI
- `ensure_collection()` now reads `settings.embedding_dim` — cannot mix providers in same collection
- Switch back to OpenAI by setting `EMBEDDING_PROVIDER=openai` in `.env`

---

## 2026-07-04 — Groq as Free LLM Fallback

**Decision:** Add `LLM_PROVIDER` env var supporting `groq` (free Llama 3.3 70B) and `openai` (GPT-4o-mini). Default to `groq` during development.

**Reason:** OpenAI key has no billing credits. Groq provides free access to Llama 3.3 70B Versatile with very generous rate limits — suitable for development and RAGAs evaluation.

**Alternatives Considered:**
- Ollama local — not available on this machine
- OpenAI billing — blocks development
- Anthropic Claude — separate key needed

**Consequences:**
- LLM factory `app/core/llm.py` wraps both providers
- Switch back to OpenAI by setting `LLM_PROVIDER=openai` in `.env`
- RAGAs evaluation will use Groq as judge LLM until OpenAI billing added

---

## 2026-07-06 — Contextual Chunking with Section Summaries

**Decision:** Prepend LLM-generated section summaries to each chunk during ingestion.

**Reason:** Chunks lose document context when isolated. Adding `[Section: Method — This section describes the QLoRA technique using 4-bit quantization.]` gives the LLM retriever and generator signal about where each chunk fits in the document.

**Alternatives Considered:**
- Metadata-only approach (section name without summary) — rejected, less informative
- Parent-child chunk hierarchy — more complex, deferred to future work
- HyDE (hypothetical document embedding) — retrieval-time, not ingestion-time

**Consequences:**
- Each chunk is ~50 tokens longer (section label overhead)
- One extra LLM call per document at ingestion time
- Section detection relies on regex — works well for standard academic paper formatting, may miss non-standard sections

---

## 2026-07-06 — Global BM25 Singleton at Ingestion

**Decision:** Build the BM25 index once at ingestion time and expose it as a module-level singleton, rather than rebuilding per query.

**Reason:** Per-query BM25 rebuild from dense results was wasteful and semantically wrong — BM25 should search the full corpus, not just the dense results. Building once at ingestion gives better recall for sparse queries.

**Alternatives Considered:**
- Per-query rebuild from dense results (previous approach) — wasteful, misses sparse-only matches
- Persistent BM25 index on disk — overkill for in-memory Qdrant
- LlamaIndex built-in BM25 — less control over the pipeline

**Consequences:**
- BM25 index lives in memory alongside Qdrant
- Server restart requires re-upload to rebuild BM25
- `build_global_bm25()` must be called after every ingestion

---

## 2026-07-06 — Custom LLM-as-Judge Instead of RAGAs Library

**Decision:** Build a custom evaluation runner using LLM-as-judge instead of using the RAGAs library.

**Reason:** RAGAs has a dependency on `langchain_community.chat_models.vertexai` which fails to import. Rather than fighting dependency issues, building a custom judge gives full control over evaluation methodology and zero external dependencies.

**Alternatives Considered:**
- Fix RAGAs import — requires installing langchain vertexai, adds complexity
- Use a different evaluation framework — none as established as RAGAs
- Manual evaluation — not scalable

**Consequences:**
- 4 metrics: faithfulness, answer_relevancy, context_precision, context_recall
- Each metric is a single LLM call with a structured prompt
- Scores are on 0-1 scale, directly comparable across configs
- Can be extended with custom metrics without library changes

---

## 2026-07-06 — Config Parameter for Ablation Isolation

**Decision:** Expose a `config` parameter on the query endpoint instead of separate endpoints per pipeline.

**Reason:** A single endpoint with a config parameter makes ablation studies trivial — same API, same request format, just different retrieval/generation strategies. Cleaner than 5 separate endpoints.

**Alternatives Considered:**
- Separate endpoints (`/query/vector_only`, `/query/hybrid`, etc.) — verbose, hard to maintain
- Query parameter on existing endpoint — less discoverable
- Internal-only config (no API exposure) — limits frontend flexibility

**Consequences:**
- Frontend can switch pipeline configs by changing one field
- Evaluation runner can hit the same endpoint with different configs
- Adding new configs requires updating one function in `query.py`

---

## 2026-07-06 — Lost-in-the-Middle Ordering

**Decision:** Order retrieved chunks so the most relevant appears last (closest to the question), using "reversed" ordering in context assembly.

**Reason:** Research shows LLMs attend better to text near the end of the context window. Placing the most relevant chunk last (just before the question) improves answer quality.

**Alternatives Considered:**
- Forward ordering (most relevant first) — worse for LLM attention
- Random ordering — no benefit
- No ordering (original retrieval order) — loses reranker signal

**Consequences:**
- Context string has most relevant chunk at the end
- Source labels still use original rank numbering
- Configurable via `order` parameter in `assemble_context()`
