# Timeline System

## Final Requirement

Timeline is confirmed as a Version 1, in-scope feature, alongside
Relationships/Links, Repository, Storage, Validation, Transactions,
Search, and offline operation (message #524, "Version 1 includes" list
under LCD-005-era Domain Model discussion). A dedicated deep-dive chapter
for Timeline specifically was not independently located in this pass —
see Recommended follow-up below.

## User Purpose

Not independently confirmed with a direct quote in this pass. Given
Timeline's placement alongside Relationships in the Version-1 feature
list, and this project's own current implementation (Timeline + Timeline
Event, with events referencing Nodes via `relatedNodeIds`), the general
purpose is almost certainly chronological/historical event tracking tied
to Nodes — but this inference is **not** independently sourced to a
specific historical passage for this document, so it is marked
accordingly.

## Required Behavior

**UNCERTAIN — not independently confirmed in this pass.**

## Relationships With Other Systems

Referenced alongside Relationships in the Version-1 scope list (message
#524), suggesting Timeline Events may reference Nodes similarly to how
Relationships do — consistent with, but not independently confirmed
against, the current implementation's `relatedNodeIds` field.

## Historical Evolution

Not independently traced for this pass.

## Recommended follow-up

If this feature area needs a confident historical answer, search
`history/08_...md` and `history/09_...md` directly for "Timeline Engine"
or "LC-TIMELINE" — the Node, Card, and Link engines each got a dedicated
multi-part spec (LC-NODE-001, LC-CARD-001, etc. — see messages #206,
#222); Timeline likely has an equivalent that wasn't located in this
pass's targeted searches.

## Status

**UNCERTAIN** — confirmed in-scope for Version 1; detailed requirements
not independently verified in this reorganization pass.

## Historical Evidence

- `history/08_...md`, message #524 (Version 1 scope list, confirms
  in-scope only)
