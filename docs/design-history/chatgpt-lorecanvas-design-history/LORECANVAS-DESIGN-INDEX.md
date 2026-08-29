# LoreCanvas Design Index — READ THIS FIRST

## What LoreCanvas is

An offline-first Android application (Kotlin + Jetpack Compose) for
writers and story planners to organize worldbuilding knowledge as
structured, interconnected data — not flat documents. Full detail:
`final-requirements/01-product-vision.md`.

## Source-of-truth hierarchy (read in this order of authority)

```
LEVEL 1 — CURRENT LORECANVAS SOURCE CODE
         What is actually implemented. The final word on implementation
         status. This archive does NOT establish implementation status
         on its own — see IMPLEMENTATION-AUDIT-MATRIX.md.
              ↓
LEVEL 2 — final-requirements/*.md
         What LoreCanvas was ultimately intended to contain, per the
         historical record. Read the specific topic file you need.
              ↓
LEVEL 3 — DESIGN-DECISION-LEDGER.md
         Why the final design became what it is — compact, one-line-
         per-decision, with status (FINAL/SUPERSEDED/FUTURE/UNCERTAIN).
              ↓
LEVEL 4 — history/*.md
         Raw chronological chat transcript. Primary evidence, but slow
         to read in full — use EVIDENCE-MAP.md to jump to the right
         part instead of reading sequentially.
              ↓
LEVEL 5 — history/00_summary_reconstruction_2026-08-23.md
         A secondary, reconstructed summary. Navigation aid ONLY — not
         authoritative if it conflicts with Levels 1-4.
```

**Never claim a feature exists merely because it appears in historical
discussion.** **Never claim a feature is missing merely because it's
absent from a summary.** Implementation status always requires actually
checking Level 1 (the real source code).

## Where things are

| Need | Go to |
|---|---|
| Quick per-feature status table | `FEATURE-INVENTORY.md` |
| Module/component design vs. implementation checklist | `ARCHITECTURE-INVENTORY.md` |
| Starter checklist for a real code audit | `IMPLEMENTATION-AUDIT-MATRIX.md` |
| "Why did we decide X" | `DESIGN-DECISION-LEDGER.md` |
| Project-specific terms (Node, Card, Link/Relationship, etc.) | `LORECANVAS-GLOSSARY.md` |
| "Where's the evidence for topic X" | `EVIDENCE-MAP.md` |
| LCD-XXX document number confusion | `LCD-INDEX.md` |
| Full chronological transcript | `history/` (13 parts + 1 summary) |

## What must NOT be assumed

- Do not convert PROPOSED → FINAL, FUTURE → REQUIRED, or UNCERTAIN →
  REQUIRED without direct evidence.
- Do not treat an LCD document number as stable on its own — read
  `LCD-INDEX.md` first; the numbering scheme was revised twice.
- Do not assume the long worldbuilding-category list (World, Locations,
  Species, Magic System, Chronicle, PlotThread, etc.) means separate
  domain classes exist for each — the final architecture almost
  certainly folds these into the generic Node's `type` field instead.
  See `final-requirements/02-core-application-model.md`.
- The original platform target was **Electron/TypeScript/React on
  Windows desktop**, not Android — this changed at some point before the
  current implementation. Don't be confused by desktop-specific language
  in early `history/` parts (e.g. "Windows Desktop," "Electron," "React"
  in `history/01_...md`).
- Command/Undo-Redo and Timeline both have **UNCERTAIN** status for
  their detailed historical specification in this archive — the pattern
  clearly exists in the *current implementation* (confirmed separately,
  by direct source inspection, outside this archive), but this archive
  itself doesn't confirm exactly what the original design conversation
  specified for them. Don't conflate "confirmed present in current code"
  with "confirmed specified in this historical archive."

## How conflicts are resolved

1. Prefer explicit later decisions over earlier proposals.
2. Prefer statements explicitly described as "final."
3. Prefer the simplified/final architecture over abandoned complexity
   (e.g. generic Node over separate worldbuilding classes).
4. Prefer explicit user decisions over assistant suggestions (e.g. the
   user picked "LoreCanvas" from a list of options ChatGPT proposed).
5. Do not resurrect rejected features.
6. Do not treat speculative/early ideas as requirements.
7. If the record doesn't establish a final decision, mark it UNCERTAIN —
   several items in this archive are marked exactly that, honestly,
   rather than guessed at.

## How to navigate the corpus efficiently

Do not read the entire `history/` archive for a typical question. Start
at `EVIDENCE-MAP.md`, find the feature area, and go directly to the
specific requirement doc and cited message range. Reading all 13
`history/` parts in full should rarely be necessary.

## Origin note

This archive was filtered from the user's entire ChatGPT conversation
history (70 conversations on every topic). Only one conversation
actually concerns LoreCanvas — auto-titled **"Most Demanded Mobile
Apps"** by ChatGPT (an artifact of its first, unrelated message before
it evolved into the real design thread) — plus one secondary summary
conversation. See `history/` directory structure for the full inclusion/
exclusion reasoning if needed.
