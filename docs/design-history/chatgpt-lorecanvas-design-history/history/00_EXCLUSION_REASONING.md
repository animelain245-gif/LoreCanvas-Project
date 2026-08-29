# LoreCanvas Design History — Index

This directory is a cleaned, filtered, chronologically-ordered conversion
of a ChatGPT conversation export into plain readable Markdown, for use as
the "historical design record" (Source B) in project audits.

## What this is

The original export was the user's **entire ChatGPT account history** —
70 conversations covering every topic they've ever discussed (gaming,
video editing, health questions, GPU troubleshooting, career advice,
unrelated coding help, etc.), packaged as one large JSON file plus
hundreds of opaque binary attachment blobs. Only **2 of those 70
conversations** actually concern LoreCanvas. This directory contains
only those 2, converted to readable Markdown, in true chronological
message order (not the raw export's internal tree/branch structure).

## Files, in reading order

| File | Covers | Messages | What it is |
|---|---|---|---|
| `00_summary_reconstruction_2026-08-23.md` | 2026-08-23 | 6 | A **secondary** summary — the user later asked ChatGPT to reconstruct what it remembered about LoreCanvas as a quick cross-check. Read this first for orientation, but treat Parts 01-13 as the primary source when they conflict. |
| `01_2026-07-14_to_2026-07-14.md` | 2026-07-14 (early) | 94 | |
| `02_2026-07-14_to_2026-07-14.md` | 2026-07-14 (later) | 84 | |
| `03_2026-07-14_to_2026-07-15.md` | 2026-07-14 → 07-15 | 68 | |
| `04_2026-07-15_to_2026-07-15.md` | 2026-07-15 | 60 | |
| `05_2026-07-15_to_2026-07-17.md` | 2026-07-15 → 07-17 | 82 | |
| `06_2026-07-17_to_2026-07-17.md` | 2026-07-17 | 72 | |
| `07_2026-07-17_to_2026-07-18.md` | 2026-07-17 → 07-18 | 66 | |
| `08_2026-07-18_to_2026-07-21.md` | 2026-07-18 → 07-21 | 108 | |
| `09_2026-07-21_to_2026-08-02.md` | 2026-07-21 → 08-02 | 96 | |
| `10_2026-08-02_to_2026-08-08.md` | 2026-08-02 → 08-08 | 86 | |
| `11_2026-08-08_to_2026-08-13.md` | 2026-08-08 → 08-13 | 40 | |
| `12_2026-08-13_to_2026-08-22.md` | 2026-08-13 → 08-22 | 69 | |
| `13_2026-08-22_to_2026-08-23.md` | 2026-08-22 → 08-23 (most recent) | 41 | |

Parts 01-13 are **one single continuous conversation** the user had with
ChatGPT about LoreCanvas, split only because it was long (966 messages,
~2.28 million characters total) — split at clean message boundaries,
capped at roughly 180,000 characters per file for practical readability,
not at arbitrary calendar boundaries. Read them in numeric order for the
real, unbroken design history — including its evolution over time (early
ideas in Part 01 may be revised or simplified in later parts; per the
audit's own Phase 2 guidance, prefer later/final statements over earlier
proposals when they conflict).

**Important labeling note:** ChatGPT auto-titled this conversation
**"Most Demanded Mobile Apps"** — an artifact of its first message, before
the conversation evolved into the actual LoreCanvas design discussion.
The title is misleading; the content, confirmed by direct inspection, is
the real design record (it contains hundreds of references to LoreCanvas
by name, the LCD-001 through LCD-017 spec documents, Node/Card/
Relationship/Timeline entities, the Command/Undo-Redo system, and
`compileSdk`/Kotlin/Compose implementation details matching the actual
repository).

## What was excluded, and why

Of the 70 conversations in the original export, 68 were excluded:

- **65 conversations** with zero LoreCanvas-related content whatsoever —
  confirmed by a full-text keyword scan (not just title matching) across
  every conversation, checking for "lorecanvas," "node," "card,"
  "relationship," "timeline," "worldbuilding," "commandhistory,"
  "undo/redo," "lcd-0," and related terms. These covered topics like
  mobile games, video editing tools, GPU troubleshooting, health
  questions, and unrelated general coding help — none of it about
  LoreCanvas.
- **"StoryPad App Specification"** (2026-07-19) — a **different,
  unrelated** throwaway test project (a simple Electron-based notepad
  app used only to test whether a different AI tool could build
  *anything at all*), explicitly described in its own text as separate
  from LoreCanvas ("a strong sign it can handle more ambitious apps like
  your planned LoreCanvas writing tool" — a passing comparison, not the
  same project).
- **"Find Unarchived Chat"** (2026-08-23) — a ChatGPT-support/navigation
  question ("I can't find an unarchived chat") with no design content;
  mentioned "lorecanvas" only because that's the chat the user was
  trying to locate.
- 2 conversations with superficial keyword overlap from unrelated
  contexts (e.g. "card" meaning a graphics card in a GPU-troubleshooting
  conversation, "timeline" meaning a video-editing timeline) — confirmed
  false positives by reading the actual content, not excluded on keyword
  count alone.

## What was NOT included from the 2 kept conversations

- ChatGPT's internal reasoning/"thoughts" traces (a separate content type
  from the visible reply text) were not extracted — these aren't part of
  what the user actually saw or acted on.
- 2 messages contained image attachments; the images themselves are not
  included (this directory is text-only), but a note marks exactly where
  an image was attached, so it's clear something was omitted rather than
  silently missing.
