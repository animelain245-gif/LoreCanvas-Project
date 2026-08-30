# UI / Navigation

## Final Requirement

Two complexity modes are required: **Simple Mode** and **Complex Mode**
(message #836, a later project-status snapshot: *"Modes: Simple Mode,
Complex Mode"*). This matches this repository's current
`Instructions_for_LoreCanvas` project-instructions file, which separately
documents this same requirement (Simple Mode for approachability, Complex
Mode for deeper worldbuilding functionality without forcing it on users
who don't need it) — cross-confirmed by two independent sources.

## User Purpose

Not every writer wants or needs the full depth of the worldbuilding
system exposed at once; Simple Mode keeps the app approachable, Complex
Mode exposes the deeper functionality for writers who want it.

## Required Behavior

Not independently traced in detail for this pass beyond the Simple/
Complex mode split. Message #836 (a project-status snapshot, not a
formal spec chapter) also notes, as of that point in the timeline:
*"Current UI: Not implemented yet"* and *"MapMaker: Planned"* — useful as
a timeline marker (this message predates whatever UI currently exists in
the repository) but not as a current-implementation claim.

## Relationships With Other Systems

Spans all entity systems (Node, Card, Relationship, Timeline) — the UI is
how a writer actually interacts with all of them.

## Historical Evolution

### Platform pivot (see `DESIGN-DECISION-LEDGER.md`, D-00X)
Early planning targeted a **Windows desktop Electron app with a React
UI** (messages #56, #512, #530), not Android. The pivot to Android/
Kotlin/Jetpack Compose — which is what the current repository actually
implements — happened at some point after this, not independently
pinpointed in this pass.

## Status

**FINAL** (Simple/Complex mode split, cross-confirmed against this
repository's own instructions). Detailed screen/navigation requirements:
**UNCERTAIN** — not independently traced in this pass.

## Historical Evidence

- `history/09_...md`, message #836 (Simple/Complex modes, project-status
  snapshot)
- `history/01_...md`, messages #56, #512, #530 (original Electron/React
  desktop platform target)
