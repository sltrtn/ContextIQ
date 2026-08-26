# Unified Design System

> Shared design language across Meluko, ContextIQ, and CodePandem.
> Each project keeps its own accent color but shares typography, spacing, radius, animation, and component patterns.

---

## ContextIQ Cross-Platform Contract

This is the binding visual language for the ContextIQ Android and React clients. It makes the product feel like one research instrument rather than two unrelated interfaces.

### One product, two appropriate renderings

| Principle | Android / Compose | React / web |
|---|---|---|
| Brand accent | Scholarly Navy `#002855` | None: pure black `#000000` and white `#FFFFFF` only |
| Theme ownership | `ContextIQTheme` controls the palette; dynamic color is off by default | `frontend/src/styles.css` owns `--ink` / `--paper`; no grays, opacity colors, or accent colors |
| Typography | Bundled Outfit family in `res/font`; `Typography` is the only type scale | Outfit via CSS; no second display font |
| Tone | Scholarly, calm, evidence-led | Editorial, high-contrast, kinetic—but still evidence-led |
| Surface model | Flat surfaces, 0dp elevation, Navy identifies primary action | Flat bordered black/white planes, inversion identifies primary action |

The platform must share hierarchy and interaction rules, not force a phone UI to imitate a desktop canvas. Android retains its Navy accent under the repository constraint; web remains strictly black and white under the frontend brief.

### Shared composition rules

1. **Evidence first.** Every research answer exposes source, page, retrieval/faithfulness state, or an explicit unavailable state.
2. **Numbered workflow.** Multi-step work is labeled `01 / SOURCE`, `02 / INTERROGATE`, `03 / METHOD`, `04 / RESPONSE` where practical. Android can use the same text in compact section headers.
3. **Uppercase utility labels.** Outfit Medium, 11–12sp/px, letter spacing around `0.1em`; reserve large type for a single clear task or result.
4. **One dominant action.** A screen has one filled/inverted primary action. Upload/select actions are secondary until a file is selected; destructive/error states never masquerade as primary actions.
5. **Flat evidence cards.** No shadows. Cards use a border or an unmistakable surface shift, title, supporting metadata, and a clear press state.
6. **Answer anatomy.** Answer text → citations/sources → machine metadata (model, latency, faithfulness). Never show fabricated result content as live output.

### Cross-platform tokens

| Token | Value | Android implementation | Web implementation |
|---|---:|---|---|
| `space-1` | 4 | `ContextIQDesign.Space.Xs` | `--space-1` |
| `space-2` | 8 | `Sm` | `--space-2` |
| `space-3` | 12 | `Md` | `--space-3` |
| `space-4` | 16 | `Lg` | `--space-4` |
| `space-5` | 20 | `Xl` / `Screen` | `--space-5` |
| `space-6` | 24 | `Xxl` | `--space-6` |
| Field radius | 12 | `ContextIQDesign.Radius.Field` | `--radius-field` |
| Card radius | 16 | `Radius.Card` | `--radius-card` |
| Action radius | 20 | `Radius.Action` | `--radius-action` |
| Standard control | 48 | `Control.StandardHeight` | `--control-height` |
| Prominent control | 56 | `Control.ProminentHeight` | 56px |

### Interaction contract

- Every tappable Android surface uses `pressScale()` with the shared values in `ContextIQDesign.Motion`; cards target `0.96`, primary buttons `0.94`, chips `0.92`, icon actions `0.90`.
- Every web control has a visible hover and a visible pressed transform. Web may invert black/white or use clipping/layout motion; it must not introduce gray or colored hover fills.
- Motion communicates state, never decoration: upload = indexing, query = retrieving, answer = reveal, error = explicit banner/text.
- Respect reduced motion on web. Android uses the existing spring implementation and must keep press feedback short.

### Implementation guardrails

- Android screens should use `MaterialTheme.typography`, `MaterialTheme.colorScheme`, and `ContextIQDesign` rather than raw colors, ad-hoc radii, or new font families. `LatexGeneratorScreen` needs its white math preview because the renderer is black-on-transparent content; it is an exception, not a general surface color.
- New Android top bars use Navy `primary`, uppercase utility title, and back navigation. New content screens start with 20dp horizontal padding and an 16–24dp vertical rhythm.
- New web views use `--ink` and `--paper` rather than new hexadecimal colors. Its API states remain explicit: no document, indexing, retrieving, response, API error.
- The active frontend is at `frontend/`; its `/api` proxy is development-only and targets port 8001. No client stores API keys.

---

## Typography

**Font: Outfit** — Clean geometric sans-serif, 9 weights.

| Role | Weight | Usage |
|---|---|---|
| Display | Black (900) | Splash screens, hero text |
| Headline | ExtraBold (800) | Screen titles, section headers |
| Title | SemiBold (600) | Card titles, modal headers |
| Body | Normal (400) | Paragraphs, descriptions, labels |
| Label | Medium (500) | Buttons, chips, badges |
| Mono | (project-specific) | Code editors, timestamps only |

### Type Scale

| Token | Size | Line Height | Letter Spacing | Weight |
|---|---|---|---|---|
| `display-lg` | 57px/sp | 64px | -0.25px | Black |
| `display-md` | 45px | 52px | 0 | Black |
| `display-sm` | 36px | 44px | 0 | Black |
| `headline-lg` | 32px | 40px | 0 | ExtraBold |
| `headline-md` | 28px | 36px | 0 | ExtraBold |
| `headline-sm` | 24px | 32px | 0 | ExtraBold |
| `title-lg` | 22px | 28px | 0 | SemiBold |
| `title-md` | 16px | 24px | 0.15px | SemiBold |
| `title-sm` | 14px | 20px | 0.1px | SemiBold |
| `body-lg` | 16px | 24px | 0.5px | Normal |
| `body-md` | 14px | 20px | 0.25px | Normal |
| `body-sm` | 12px | 16px | 0.4px | Normal |
| `label-lg` | 14px | 20px | 0.1px | Medium |
| `label-md` | 12px | 16px | 0.5px | Medium |
| `label-sm` | 11px | 16px | 0.5px | Medium |

### Uppercase Headers

Section labels and chip text use uppercase with `letter-spacing: 0.1em` across all projects.

---

## Color System

Each project defines its own accent but shares all neutral tokens.

### Shared Neutrals (Dark Mode)

| Token | Hex | Usage |
|---|---|---|
| `--bg` | `#0F0F12` | Page/screen background |
| `--surface` | `#18181C` | Card/panel surface |
| `--surface-raised` | `#222226` | Elevated surface, hover states |
| `--border` | `#2A2A30` | Borders, dividers, outlines |
| `--text` | `#EDE0DA` | Primary text (warm off-white) |
| `--text-dim` | `#8A8080` | Secondary/muted text |
| `--text-inverse` | `#0F0F12` | Text on accent-colored surfaces |

### Shared Neutrals (Light Mode)

| Token | Hex | Usage |
|---|---|---|
| `--bg` | `#FFFCF8` | Page/screen background |
| `--surface` | `#FAEDE6` | Card/panel surface |
| `--surface-raised` | `#F4E7DF` | Elevated surface, hover states |
| `--border` | `#D8C2BC` | Borders, dividers, outlines |
| `--text` | `#201B18` | Primary text |
| `--text-dim` | `#85736D` | Secondary/muted text |
| `--text-inverse` | `#FFFCF8` | Text on accent-colored surfaces |

### Semantic Colors (shared)

| Token | Hex | Usage |
|---|---|---|
| `--success` | `#4ade80` | Correct, won, positive |
| `--warning` | `#facc15` | Caution, partial, time low |
| `--error` | `#f87171` | Wrong, lost, danger |

### Project Accent Colors

| Project | Accent | Hex | On-Accent |
|---|---|---|---|
| Meluko | Sunrise Amber | `#FF8C00` | `#FFFFFF` |
| ContextIQ | Scholarly Navy | `#002855` | `#FFFFFF` |
| CodePandem | Battle Indigo | `#6c63ff` | `#FFFFFF` |

---

## Spacing Scale

Base unit: **4px**. All spacing is a multiple of 4.

| Token | Value | Usage |
|---|---|---|
| `xs` | 4px | Tight gaps, icon padding |
| `sm` | 8px | Inline gaps, small padding |
| `md` | 12px | Card inner padding (compact) |
| `lg` | 16px | Card padding, input padding |
| `xl` | 20px | Section gaps |
| `2xl` | 24px | Large section gaps |
| `3xl` | 32px | Screen horizontal padding |
| `4xl` | 40px | Major vertical gaps |
| `5xl` | 48px | Hero spacing |

---

## Border Radius

| Token | Value | Usage |
|---|---|---|
| `radius-sm` | 8px / 8dp | Small elements (chips, badges) |
| `radius-md` | 12px / 12dp | Cards, inputs |
| `radius-lg` | 16px / 16dp | Large cards, modals |
| `radius-xl` | 20px / 20dp | Buttons, bottom sheets |
| `radius-full` | 9999px | Pill shapes, avatars |

---

## Component Patterns

### Cards
- **Elevation:** 0 (flat, no shadow)
- **Border:** 1px solid `--border`
- **Radius:** `radius-md` (12px) to `radius-lg` (16px)
- **Padding:** `lg` (16px) to `xl` (20px)
- **Background:** `--surface`

### Buttons
- **Height:** 48px (standard) to 56px (prominent)
- **Radius:** `radius-xl` (20px)
- **Font:** Outfit Medium (500), `label-lg` (14px)
- **Padding:** 0 24px
- **Fill:** Accent color background, white text
- **Hover:** 8% lighter or `--surface-raised` for outline variant
- **Disabled:** 40% opacity
- **Transition:** background 150ms ease

### Inputs
- **Radius:** `radius-md` (12px)
- **Border:** 1px solid `--border`, accent on focus
- **Padding:** 12px 16px
- **Font:** Outfit Normal (400), `body-md` (14px)
- **Background:** `--bg`

### Bottom Sheets / Modals
- **Top radius:** `radius-xl` (20px)
- **Background:** `--surface`
- **No drag handle** (Meluko/ContextIQ convention)

---

## Animation Language

### Android (Compose)
- **Spring-based press** on all interactive elements
- Default: scale to 0.94, `DampingRatioNoBouncy`, `StiffnessMedium`
- Chips: scale to 0.92, `DampingRatioLowBouncy`
- FABs: scale to 0.90, `DampingRatioMediumBouncy`, lower stiffness
- Implementation: `Animatable` + `graphicsLayer` (avoid recomposition)

### Web (CSS)
- **Hover transitions:** 150ms ease for background/color changes
- **Scale on press:** `transform: scale(0.97)` with `transition: transform 100ms ease`
- **Focus rings:** 2px accent outline, 2px offset
- **Loading states:** CSS spinner (40px, accent border-top, 0.8s linear infinite)
- **Low-time pulse:** opacity 1 -> 0.5, 0.5s ease-in-out infinite alternate

---

## Dark Mode Approach

- **Default:** Dark mode
- **Background:** `#0F0F12` (shared near-black, NOT true black)
- **Surface hierarchy:** bg -> surface -> surface-raised (3-tier)
- **Text:** Warm off-white `#EDE0DA` (not pure white)
- **Borders:** Subtle `#2A2A30` (not harsh white/gray)
- **Light mode:** Warm cream tones (see light neutrals above)

---

## Logo Treatment

All projects use their name as a wordmark in **Outfit Black** with `letter-spacing: 0.05em`. No icon/logo mark — the typography IS the brand.

---

## What Does NOT Change Per Project

| Stays Shared | Changes Per Project |
|---|---|
| Outfit font | Accent color |
| Type scale | App-specific components |
| Spacing scale | Content and copy |
| Border radius tokens | Illustrations/icons |
| Neutral color tokens | — |
| Semantic colors | — |
| Component patterns | — |
| Animation language | — |
| Dark mode approach | — |
| Uppercase header convention | — |
