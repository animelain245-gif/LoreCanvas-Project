# Node System

## Final Requirement

The Node is the identity/existence layer: it establishes that something
exists in the project and what general kind of thing it is. See
`02-core-application-model.md` for why this is generic rather than
category-specific.

## User Purpose

Gives every worldbuilding entity — regardless of category — a stable,
addressable identity that Cards, Relationships, and Timeline Events can
all reference uniformly.

## Required Behavior

- "The Node Engine defines identity." Every entity in a project is
  represented by exactly one Node (Chapter 10, Node Engine, message
  #436).
- A later, more technical pass ("Chapter 5 — Node Model Specification,"
  message #458) formalizes this into an implementable spec — exact field
  list not independently re-verified for this reorganization pass; see
  `history/08_2026-07-18_to_2026-07-21.md` message #458 directly if the
  precise historical field list is needed for a future task.

## Data / Domain Requirements

Per Chapter 10 (message #436) and general usage throughout the record:
- Identity (id)
- `type` (free string — see `02-core-application-model.md`)
- Name
- Some form of status/lifecycle field (referenced elsewhere in the
  conversation as supporting archive/restore)
- Tags

## UI Requirements

Not independently confirmed in the excerpts read for this pass — see
`history/` directly for Workspace/Node-editor discussion if UI-level
historical detail is needed (Chapter 10 of "Chapter 0" pass, message
#470, "Workspace Specification," is a likely source).

## Relationships With Other Systems

- Cards attach to a Node (`05-card-system.md`)
- Relationships connect two Nodes (`07-relationship-system.md`)
- Timeline Events can reference Nodes (`08-timeline-system.md`)

## Historical Evolution

### Earlier Proposal
"Map Node" — a point placed on a map (message #17). Narrower and
map-specific.

### Revision
Generalized from a map-specific concept into the project-wide identity
concept described in `02-core-application-model.md`.

### Final Decision
Chapter 10, "Node Engine" (message #436) states the generalized version
as settled.

## Status

**FINAL** (the generic-identity concept itself). Exact final field list:
**UNCERTAIN** — not independently re-verified against the later, more
technical Chapter 5 pass (message #458) for this reorganization task.

## Historical Evidence

- `history/08_2026-07-18_to_2026-07-21.md`, messages #436, #458
- `history/01_2026-07-14_to_2026-07-14.md`, message #17
