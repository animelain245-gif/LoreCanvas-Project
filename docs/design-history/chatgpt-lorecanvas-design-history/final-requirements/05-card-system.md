# Card System

## Final Requirement

Cards are the primary knowledge containers of LoreCanvas — where actual
content lives, as opposed to the Node, which only establishes that
something exists.

## User Purpose

Direct paraphrase from Chapter 11, "Card Engine" (message #436): if
Nodes answer "What exists?", Cards answer "What do we know about it?"
Knowledge should be modular rather than one enormous document — a
Character Node might have separate Cards for Biography, Appearance,
Personality, Inventory, Relationships, and Notes, each independently
reusable and searchable.

## Required Behavior

- A Card attaches to exactly one Node (parent).
- Multiple Cards can attach to the same Node, each covering a different
  facet of knowledge about it.
- Cards are described as "modular" and "reusable, searchable, and
  independent" (Chapter 11, message #436) — implying Cards should be
  addressable/searchable as their own unit, not just as part of their
  parent Node.

## Data / Domain Requirements

Not independently re-verified field-by-field for this pass against the
more technical "Chapter 6 — Card Model Specification" (message #460).
Known from Chapter 11's philosophy section: a Card has a type/category
(matching the modular-facets idea: Biography, Appearance, Personality,
Inventory, Relationships, Notes) and content.

## Relationships With Other Systems

Attaches to exactly one Node (`04-node-system.md`).

## Historical Evolution

### Final Decision
Chapter 11, "Card Engine" (message #436) states the modular-knowledge
philosophy directly as settled design, not a proposal under discussion.

## Status

**FINAL** (the modular-content philosophy). Exact field list: **UNCERTAIN**
-- see message #460 directly for the more technical spec pass if needed.

## Historical Evidence

- `history/08_2026-07-18_to_2026-07-21.md`, messages #436 (Chapter 11
  philosophy), #460 (Chapter 6 technical spec, not independently
  re-verified for this pass)
