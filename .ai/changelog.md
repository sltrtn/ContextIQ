# Changelog

> Human-readable summary of major repository changes. Not every commit — only meaningful milestones.

---

## 2026-07-02 — Project Memory System

**Created:** `.ai/` directory with full project memory: project.md, roadmap.md, current_task.md, progress.md, decisions.md, handoff.md, changelog.md.
**Created:** `AGENTS.md` with permanent project instructions.

---

## 2026-07-02 — Scholium → ContextIQ Migration

**Package rename:** `com.example.scholium` → `com.contextiq.app`.
**App rename:** "Scholium" → "ContextIQ".
**Design language:** Meluko-inspired with Scholarly Navy (#002855) hero color, Clash Display fonts, spring animations, rounded corners.
**Networking:** Retrofit layer with 13 API endpoints replacing direct OkHttp calls.
**Security:** Removed hardcoded Sarvam API key. All AI calls now go through backend.
**Screens:** All 14 screens rewired to `ContextIQClient.api` singleton.
**Theme:** `Theme.ContextIQ` with dark/light schemes.

---

## 2026-07-02 — Day 0 Backend Scaffold

**Created:** `backend/` directory with FastAPI app skeleton, Pydantic config, connection test script, .env template, requirements.txt.
**Updated:** `.gitignore` for backend artifacts.
**Blocked:** Waiting for API keys to verify connections.

---

## 2026-07-02 — Repository Created

Forked from Scholium (`arnavt1605/Scholium`). Initial commit with ContextIQ branding and all migrated Android code.
