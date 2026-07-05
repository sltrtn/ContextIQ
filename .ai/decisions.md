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
