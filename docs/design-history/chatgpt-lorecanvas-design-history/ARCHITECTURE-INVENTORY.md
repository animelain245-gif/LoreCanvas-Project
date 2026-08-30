# LoreCanvas — Architecture Inventory

Per-component design intent vs. implementation status. Implementation
status is intentionally left as `REQUIRES CODE AUDIT` throughout — this
document does not itself inspect current source code (per this task's
scope: documentation restructuring only).

## Module structure

**DESIGNED:** A layered, modular architecture with dependencies flowing
downward: UI → Workspace → Repository → Project (message #504,
"Dependency Rules"). Package Architecture (LCD-004) formalizes this into
"every file belongs to a clearly defined package with a single
responsibility" (message #604).

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT (this repository's actual
current module list — `app`, `core-common`, `core-domain`,
`core-validation`, `core-events`, `core-commands`, `core-storage`,
`core-repository`, `core-search`, `core-graph`, `core-plugin` — was
established by direct inspection in earlier, separate work on this
project, not from this ChatGPT archive; cross-checking that module list
against the historical Package Architecture spec in detail was not
performed in this pass.)

## Domain model

**DESIGNED:** Generic Node + attached Cards + Relationships between
Nodes + Timeline. See `final-requirements/02-core-application-model.md`.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT (though prior, separate
work on this repository has already confirmed `Node`, `Card`,
`Relationship`/`RelationshipContext`, `Timeline`/`TimelineEvent`, and
`Template` exist as real domain classes in `core-domain` — this
documentation pass does not re-derive that independently).

## Repository layer

**DESIGNED:** Central authority for all project-data mutation; no
component outside it may directly create/modify/delete project data
(message #612).

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Storage

**DESIGNED:** See `final-requirements/23-storage-persistence.md`.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Commands / Undo-Redo

**DESIGNED:** UNCERTAIN — see `final-requirements/22-command-undo-redo.md`.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Validation

**DESIGNED:** Referenced as a distinct engine/chapter (message #480,
#256) but not independently traced in full detail for this pass.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Search

**DESIGNED:** Referenced ("Search Experience" chapter, LCD-008, message
#622) but not independently traced in detail for this pass.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Graph

**DESIGNED:** Referenced ("graph relationship model," message #430) but
not independently traced in detail for this pass.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Events

**DESIGNED:** Referenced (message #314, discussing whether an event
system affects offline operation — concluded "No. Events are internal.")

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Plugins

**DESIGNED:** "Designed but Deferred" per ADR-007 (message #450) — the
architecture was planned, but implementation was explicitly deferred
beyond Version 1.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT (note: a `core-plugin`
module already exists in this repository per prior separate work — worth
checking during a real audit whether it represents genuine functionality
or scaffolding only, given the historical record explicitly describes
this as deferred).

## UI state / navigation

**DESIGNED:** Simple Mode / Complex Mode split (message #836). Detailed
screen inventory not independently traced in this pass.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT.

## Persistence / offline-first design

**DESIGNED:** See `final-requirements/25-offline-first-requirements.md`
— the single most consistently and thoroughly documented requirement in
the entire historical record.

**IMPLEMENTATION STATUS:** REQUIRES CODE AUDIT (though separate, prior
work on this repository has already confirmed no `INTERNET` permission
is declared in the Android manifest — a strong positive signal, though
not re-derived independently in this documentation-focused pass).

## Platform (Android vs. original Electron target)

**DESIGNED (originally):** Electron + TypeScript + React, Windows
desktop (messages #56, #512, #530).

**DESIGNED (current):** Kotlin + Jetpack Compose, native Android — the
platform pivot itself is not independently traced to a specific decision
point in this pass; see `DESIGN-DECISION-LEDGER.md` D-004.

**IMPLEMENTATION STATUS:** Confirmed Android/Kotlin/Compose by this
repository's actual `build.gradle.kts` (established in prior, separate
work — not re-derived in this pass).
