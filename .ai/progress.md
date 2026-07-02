# ContextIQ — Progress Log

> Chronological work log. Append entries as milestones are reached.

---

## 2026-07-02 — Initial Project Memory Setup

Created the `.ai/` project memory system and `AGENTS.md` with permanent project instructions.

**Added:**
- `.ai/project.md` — project overview, architecture, external services
- `.ai/roadmap.md` — completed milestones, current milestone, future milestones
- `.ai/current_task.md` — Day 0 setup task
- `.ai/progress.md` — this file
- `.ai/decisions.md` — architectural decisions log
- `.ai/changelog.md` — human-readable major changes
- `.ai/handoff.md` — session handoff for next agent
- `AGENTS.md` — permanent project instructions

---

## 2026-07-02 — Scholium → ContextIQ Migration (Previous Work)

Migrated the entire Scholium Android app to ContextIQ with new design language and Retrofit networking.

**Added:**
- Retrofit network layer: `ContextIQApi.kt` (13 endpoints), `ContextIQClient.kt`, DTOs, `AuthInterceptor.kt`
- New design system: Scholarly Navy (#002855), Clash Display fonts, spring animations
- 14 redesigned screens with Retrofit integration
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- `ui/components/ExpressiveUtils.kt` with `pressScale()`
- `domain/UiState.kt` sealed class

**Changed:**
- Package: `com.example.scholium` → `com.contextiq.app`
- Theme: `Theme.Scholium` → `Theme.ContextIQ`
- App name: "Scholium" → "ContextIQ"
- Database: "scholium_database" → "contextiq_database"
- All 34 Kotlin files moved to new package

**Deleted:**
- `SarvamApiService.kt` (compromised API key removed)
- Old theme files under `com.example.scholium`
- Direct Gemini API calls (moved to backend)

**Security:**
- Removed hardcoded Sarvam API key `sk_59k2cw5q_rSbUWFbJ4OeexGxuE4g4IX4Z`
- All AI calls now go through backend — no keys on device

## 2026-07-02 — Day 0 Backend Scaffold

Set up the backend project structure and Python environment.

**Added:**
- `backend/` directory with full subpackage structure
- `backend/app/core/config.py` — Pydantic settings via .env
- `backend/app/main.py` — FastAPI app with health endpoint
- `backend/test_connections.py` — OpenAI + Qdrant + Cohere verification
- `backend/.env` (template and empty file)
- `backend/requirements.txt` — all dependencies frozen
- `data/papers/` directory for test PDFs

**Changed:**
- `.gitignore` — added backend/Python entries

**Blocked:** Waiting for API keys from user to run `test_connections.py`.

---

**Known Issues:**
- `PaperAnalyzerScreen.kt` uses fully qualified `com.contextiq.app.network.ContextIQClient` references (not idiomatic imports)
- Room DB uses `fallbackToDestructiveMigration()` — needs proper migration
- Sarvam key still needs rotation at Sarvam dashboard
