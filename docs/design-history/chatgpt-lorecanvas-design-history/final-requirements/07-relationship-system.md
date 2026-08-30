# Relationship System

## Final Requirement

Relationships connect two Nodes and explain *how* they're connected, not
just *that* they're connected. Originally specified under the name
"Link" — see Historical Evolution.

## User Purpose

Direct paraphrase from Chapter 7, "Link Model Specification" (message
#462): a relationship is knowledge, and deserves the same architectural
importance as a Node or a Card. Without relationships, LoreCanvas is a
collection of independent objects; with them, it becomes a connected
world.

## Required Behavior

- Relationships have identity (their own id, not just a foreign-key
  pair).
- Relationships have properties (not just "A connects to B," but
  descriptive data about the connection).
- Explicitly stated as first-class, not a lightweight join/pivot
  construct.

## Data / Domain Requirements

Not independently re-verified field-by-field for this pass. Known from
the philosophy section: identity + properties, connecting exactly two
Nodes.

## Relationships With Other Systems

Connects exactly two Nodes (`04-node-system.md`), regardless of their
`type` (per the generic-Node model in `02-core-application-model.md`).

## Historical Evolution

### Earlier Proposal / Terminology
Originally specified as **"Link"** ("Chapter 7 — Link Model
Specification," message #462), not "Relationship."

### Revision
Terminology changed from "Link" to "Relationship" at some point after
this — the current implementation and later parts of the historical
record consistently use "Relationship," not "Link." The exact message
where this rename happened was not independently located for this pass.

### Final Decision
The *concept* (first-class, identity-bearing connections with
properties) is stated as settled in Chapter 7; only the *name* changed
afterward.

## Status

**FINAL** (concept). Terminology: **RENAMED** (Link → Relationship,
exact transition point UNCERTAIN).

## Historical Evidence

- `history/08_2026-07-18_to_2026-07-21.md`, message #462 (Chapter 7,
  under the name "Link Model Specification")
