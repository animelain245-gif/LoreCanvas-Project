# Storage / Persistence

## Final Requirement

Project data persists as structured, human-inspectable local files, not
a single opaque database blob. Six stated database philosophy
principles, the first being: "Projects own everything. Nothing exists
outside a Project" (message #156).

## User Purpose

Writers may work on a single project for years; storage must be
reliable, recoverable, and durable over that timescale without depending
on any external service.

## Required Behavior

- "Storage is: Deterministic, Offline-first, Versioned, Recoverable,
  Human-inspectable where practical" (message #522, "Storage
  Philosophy").
- "Projects remain portable between machines" (message #522).
- Autosave is a named, dedicated concern ("Chapter 11 — Autosave
  Manager," message #618, under LCD-007 "Storage Engine": *"Autosave
  protects the cre[ator's work]..."* — text cut off in the excerpt read
  for this pass, meaning is clear from context but the exact chapter
  text wasn't fully re-read).
- Import should treat external data as untrusted (see
  `25-offline-first-requirements.md`'s security-guidance evidence,
  messages #638, #648) — validate all imported data, prevent path
  traversal, sanitize external inputs.
- Transactions are atomic (message #480, #504): "Transactions are
  atomic... Validation precedes persistence."

## Data / Domain Requirements

Not independently re-verified field-by-field against the full LCD-007
Storage Engine spec for this pass — see `history/09_...md` messages
#616–#618 directly if the complete original spec is needed.

## Relationships With Other Systems

Underlies every entity system (Node, Card, Relationship, Timeline,
Template) — all persist through this layer.

## Historical Evolution

### Earlier Proposal
Early planning (messages #148, #156) already established
"Projects own everything" as a core principle, under an earlier
architecture-document numbering ("LC-ARCH-" prefix, message #148) that
predates both the LAR-001 and LCD-XXX schemes documented in
`LCD-INDEX.md`.

### Final Decision
LCD-007 "Storage Engine" (message #616) is the most complete, latest
formal specification located in this pass.

## Status

**FINAL** (philosophy/principles). Exact field-level spec: **UNCERTAIN**
— not independently re-verified in full for this pass.

## Historical Evidence

- `history/01_...md`, messages #148, #156
- `history/07_...md`–`history/08_...md`, messages #480, #504, #522
- `history/09_2026-07-21_to_2026-08-02.md`, messages #616, #618, #638,
  #648
